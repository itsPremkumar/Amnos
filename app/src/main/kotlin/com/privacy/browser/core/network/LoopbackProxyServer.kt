package com.privacy.browser.core.network

import com.privacy.browser.core.session.AmnosLog
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoopbackProxyServer(
    private val networkSecurityManager: NetworkSecurityManager,
    private val onTunnelOpened: (id: String, host: String, port: Int) -> Unit,
    private val onTunnelClosed: (id: String) -> Unit
) {
    private val workerPool: ExecutorService = Executors.newCachedThreadPool()
    private val sockets = CopyOnWriteArrayList<Socket>()
    @Volatile
    private var serverSocket: ServerSocket? = null

    val isRunning: Boolean
        get() = serverSocket?.isClosed == false

    val port: Int?
        get() = serverSocket?.localPort?.takeIf { it > 0 }

    fun start(): Int {
        if (isRunning) {
            return port ?: 0
        }

        val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        workerPool.execute {
            acceptLoop(socket)
        }
        return socket.localPort
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {
        }
        sockets.forEach {
            try {
                it.close()
            } catch (ignored: Exception) {
            }
        }
        sockets.clear()
        serverSocket = null
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            try {
                val client = socket.accept()
                sockets.add(client)
                workerPool.execute {
                    handleClient(client)
                }
            } catch (_: SocketException) {
                break
            } catch (error: Exception) {
                AmnosLog.e("LoopbackProxy", "Proxy accept failed", error)
            }
        }
    }

    private fun handleClient(client: Socket) {
        client.soTimeout = 15_000
        try {
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            val requestLine = readLine(input) ?: return
            if (requestLine.isBlank()) return

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(input) ?: break
                if (line.isBlank()) break
                val separator = line.indexOf(':')
                if (separator > 0) {
                    headers[line.substring(0, separator).trim()] = line.substring(separator + 1).trim()
                }
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                writeSimpleResponse(output, 400, "Bad Request", "Malformed proxy request")
                return
            }

            val method = parts[0].uppercase()
            val target = parts[1]

            if (method == "CONNECT") {
                handleConnect(client, output, target)
                return
            }

            writeSimpleResponse(
                output = output,
                code = 451,
                message = "HTTPS Only",
                body = "Amnos loopback proxy rejects non-CONNECT requests."
            )
        } catch (_: SocketException) {
        } catch (error: Exception) {
            AmnosLog.e("LoopbackProxy", "Proxy client failed", error)
        } finally {
            sockets.remove(client)
            try {
                client.close()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun handleConnect(
        client: Socket,
        output: BufferedOutputStream,
        target: String
    ) {
        val host = target.substringBefore(':').trim().removePrefix("[").removeSuffix("]")
        val port = target.substringAfter(':', "443").toIntOrNull() ?: 443
        val id = UUID.randomUUID().toString()

        if (!networkSecurityManager.isTunnelAllowed(host, port)) {
            writeSimpleResponse(output, 403, "Forbidden", "Tunnel blocked by Amnos")
            return
        }

        val remote = Socket()
        try {
            val addresses = DnsManager.lookup(host, blockIpv6 = true)
            val connected = addresses.any { address ->
                try {
                    remote.connect(InetSocketAddress(address, port), 15_000)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            if (!connected) {
                writeSimpleResponse(output, 502, "Bad Gateway", "Unable to resolve or connect to upstream host")
                return
            }

            output.write("HTTP/1.1 200 Connection Established\r\nProxy-Agent: Amnos\r\n\r\n".toByteArray())
            output.flush()

            onTunnelOpened(id, host, port)

            val upstream = workerPool.submit {
                pipe(client.getInputStream(), remote.getOutputStream())
            }
            val downstream = workerPool.submit {
                pipe(remote.getInputStream(), client.getOutputStream())
            }

            upstream.get()
            downstream.get()
        } catch (error: Exception) {
            AmnosLog.e("LoopbackProxy", "CONNECT tunnel failed for $host:$port", error)
        } finally {
            onTunnelClosed(id)
            try {
                remote.close()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun pipe(input: java.io.InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        try {
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                output.write(buffer, 0, count)
                output.flush()
            }
        } catch (_: SocketException) {
        }
    }

    private fun writeSimpleResponse(
        output: BufferedOutputStream,
        code: Int,
        message: String,
        body: String
    ) {
        val bodyBytes = body.toByteArray()
        val response = buildString {
            append("HTTP/1.1 $code $message\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Connection: close\r\n")
            append("Cache-Control: no-store\r\n")
            append("\r\n")
        }.toByteArray()
        output.write(response)
        output.write(bodyBytes)
        output.flush()
    }

    private fun readLine(input: BufferedInputStream): String? {
        val builder = StringBuilder()
        while (true) {
            val next = input.read()
            if (next == -1) {
                return if (builder.isEmpty()) null else builder.toString()
            }
            if (next == '\n'.code) {
                return builder.toString().trimEnd('\r')
            }
            builder.append(next.toChar())
        }
    }
}
