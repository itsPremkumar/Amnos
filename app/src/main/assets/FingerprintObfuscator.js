(function() {
    if (window.__amnosInjected) {
        return;
    }

    Object.defineProperty(window, "__amnosInjected", {
        value: true,
        configurable: false,
        enumerable: false,
        writable: false
    });

    const config = window._privacyConfig || {};
    const policy = config.policy || {};
    const noop = function() {};
    const nativeWebSocket = window.WebSocket;
    const nativeDateNow = Date.now.bind(Date);
    const nativeRequestAnimationFrame = window.requestAnimationFrame ? window.requestAnimationFrame.bind(window) : null;
    const nativePerformanceNow = window.performance && window.performance.now ? window.performance.now.bind(window.performance) : null;
    const strictFingerprinting = policy.fingerprintLevel === "STRICT";

    const denyPromise = function(message) {
        return Promise.reject(new DOMException(message || "Blocked by Amnos", "SecurityError"));
    };
    const throwSecurity = function(message) {
        throw new DOMException(message || "Blocked by Amnos", "SecurityError");
    };
    const defineGetter = function(target, property, getter) {
        try {
            Object.defineProperty(target, property, {
                get: getter,
                configurable: true
            });
        } catch (ignored) {}
    };
    const defineValue = function(target, property, value) {
        try {
            Object.defineProperty(target, property, {
                value: value,
                configurable: true,
                writable: false
            });
        } catch (ignored) {}
    };
    const safePost = function(payload) {
        try {
            if (window.amnosBridge && typeof window.amnosBridge.postMessage === "function") {
                window.amnosBridge.postMessage(JSON.stringify(payload));
            }
        } catch (ignored) {}
    };
    const siteKey = function(hostname) {
        if (!hostname) {
            return "";
        }
        const normalized = hostname.toLowerCase().replace(/\.$/, "");
        const labels = normalized.split(".").filter(Boolean);
        if (labels.length < 2) {
            return normalized;
        }
        return labels.slice(-2).join(".");
    };
    const sameSite = function(left, right) {
        return siteKey(left) === siteKey(right);
    };
    const quantizeTime = function(value) {
        const resolution = Number(policy.timingResolutionMs || 16);
        const jitter = Number(policy.timingJitterMs || 0);
        const noise = (((config.noiseSeed || 1) % 997) / 997) - 0.5;
        return Math.round(value / resolution) * resolution + (noise * jitter);
    };
    const freezeList = function(values) {
        return Object.freeze(values.slice());
    };

    const navigatorProto = Object.getPrototypeOf(navigator);
    const screenProto = Object.getPrototypeOf(screen);

    defineGetter(navigatorProto, "userAgent", function() { return config.userAgent; });
    defineGetter(navigatorProto, "platform", function() { return config.platform; });
    defineGetter(navigatorProto, "language", function() { return config.languages[0]; });
    defineGetter(navigatorProto, "languages", function() { return freezeList(config.languages); });
    defineGetter(navigatorProto, "hardwareConcurrency", function() { return config.hardwareConcurrency; });
    defineGetter(navigatorProto, "deviceMemory", function() { return config.deviceMemory; });
    defineGetter(navigatorProto, "webdriver", function() { return false; });
    defineGetter(navigatorProto, "doNotTrack", function() { return "1"; });
    defineGetter(navigatorProto, "plugins", function() { return freezeList([]); });
    defineGetter(navigatorProto, "mimeTypes", function() { return freezeList([]); });
    defineValue(navigator, "globalPrivacyControl", true);

    defineGetter(screenProto, "width", function() { return config.screen.width; });
    defineGetter(screenProto, "height", function() { return config.screen.height; });
    defineGetter(screenProto, "availWidth", function() { return config.screen.availWidth; });
    defineGetter(screenProto, "availHeight", function() { return config.screen.availHeight; });
    defineGetter(screenProto, "colorDepth", function() { return config.screen.colorDepth; });
    defineGetter(screenProto, "pixelDepth", function() { return config.screen.pixelDepth; });
    defineGetter(window, "devicePixelRatio", function() { return config.screen.devicePixelRatio; });

    if (window.Intl && Intl.DateTimeFormat && Intl.DateTimeFormat.prototype) {
        const originalResolvedOptions = Intl.DateTimeFormat.prototype.resolvedOptions;
        Intl.DateTimeFormat.prototype.resolvedOptions = function() {
            const options = originalResolvedOptions.apply(this, arguments);
            options.timeZone = config.timeZone;
            return options;
        };
    }

    if (Date.prototype.getTimezoneOffset) {
        Date.prototype.getTimezoneOffset = function() {
            return config.timezoneOffsetMinutes;
        };
    }

    Date.now = function() {
        return quantizeTime(nativeDateNow());
    };

    if (window.performance && nativePerformanceNow) {
        window.performance.now = function() {
            return quantizeTime(nativePerformanceNow());
        };
    }

    if (nativeRequestAnimationFrame) {
        window.requestAnimationFrame = function(callback) {
            return nativeRequestAnimationFrame(function(timestamp) {
                callback(quantizeTime(timestamp));
            });
        };
    }

    const blockedStorage = {
        getItem: function() { return null; },
        setItem: function() { throwSecurity("Storage disabled"); },
        removeItem: noop,
        clear: noop,
        key: function() { return null; },
        length: 0
    };
    defineGetter(window, "localStorage", function() { return blockedStorage; });
    defineGetter(window, "sessionStorage", function() { return blockedStorage; });
    defineValue(window, "openDatabase", undefined);
    defineGetter(window, "indexedDB", function() { return undefined; });
    defineValue(window, "caches", Object.freeze({
        open: function() { return denyPromise("Cache API disabled"); },
        match: function() { return Promise.resolve(undefined); },
        delete: function() { return Promise.resolve(false); },
        keys: function() { return Promise.resolve([]); }
    }));

    if (navigator.storage) {
        defineValue(navigator, "storage", Object.freeze({
            estimate: function() { return Promise.resolve({ usage: 0, quota: 0 }); },
            persisted: function() { return Promise.resolve(false); },
            persist: function() { return Promise.resolve(false); }
        }));
    }

    if (navigator.permissions && navigator.permissions.query) {
        const originalQuery = navigator.permissions.query.bind(navigator.permissions);
        navigator.permissions.query = function(descriptor) {
            const name = descriptor && descriptor.name;
            if (name && [
                "camera",
                "microphone",
                "geolocation",
                "clipboard-read",
                "clipboard-write",
                "accelerometer",
                "gyroscope",
                "magnetometer"
            ].indexOf(name) >= 0) {
                return Promise.resolve({ state: "denied", onchange: null });
            }
            return originalQuery(descriptor);
        };
    }

    if (navigator.geolocation) {
        const geoError = function(errorCallback) {
            if (errorCallback) {
                errorCallback({
                    code: 1,
                    message: "Geolocation blocked",
                    PERMISSION_DENIED: 1,
                    POSITION_UNAVAILABLE: 2,
                    TIMEOUT: 3
                });
            }
        };
        navigator.geolocation.getCurrentPosition = function(success, error) { geoError(error); };
        navigator.geolocation.watchPosition = function(success, error) { geoError(error); return 0; };
    }

    if (navigator.mediaDevices) {
        navigator.mediaDevices.getUserMedia = function() {
            safePost({ type: "webrtc", detail: "getUserMedia", blocked: true });
            return denyPromise("Media devices blocked");
        };
        navigator.mediaDevices.enumerateDevices = function() {
            return Promise.resolve([]);
        };
    }

    defineValue(window, "DeviceMotionEvent", undefined);
    defineValue(window, "DeviceOrientationEvent", undefined);
    defineValue(window, "Accelerometer", undefined);
    defineValue(window, "Gyroscope", undefined);
    defineValue(window, "Magnetometer", undefined);
    defineValue(window, "AbsoluteOrientationSensor", undefined);
    defineValue(window, "RelativeOrientationSensor", undefined);

    if (navigator.clipboard) {
        defineValue(navigator, "clipboard", Object.freeze({
            readText: function() { return denyPromise("Clipboard blocked"); },
            writeText: function() { return denyPromise("Clipboard blocked"); },
            read: function() { return denyPromise("Clipboard blocked"); },
            write: function() { return denyPromise("Clipboard blocked"); }
        }));
    }

    if (navigator.getBattery) {
        navigator.getBattery = function() {
            return Promise.resolve({
                charging: true,
                chargingTime: 0,
                dischargingTime: Infinity,
                level: 0.76,
                addEventListener: noop,
                removeEventListener: noop
            });
        };
    }

    if (navigator.connection) {
        const connectionProto = Object.getPrototypeOf(navigator.connection);
        defineGetter(connectionProto, "effectiveType", function() { return "4g"; });
        defineGetter(connectionProto, "downlink", function() { return 10; });
        defineGetter(connectionProto, "rtt", function() { return 50; });
        defineGetter(connectionProto, "saveData", function() { return false; });
    }

    if (navigator.getGamepads) {
        navigator.getGamepads = function() { return []; };
    }

    if (policy.blockServiceWorkers && navigator.serviceWorker) {
        defineValue(navigator, "serviceWorker", Object.freeze({
            register: function() { return denyPromise("Service workers blocked"); },
            getRegistration: function() { return Promise.resolve(undefined); },
            getRegistrations: function() { return Promise.resolve([]); },
            ready: denyPromise("Service workers blocked")
        }));
    }

    if (policy.blockWebRtc) {
        const FakeRTCSessionDescription = function(init) {
            return Object.assign({ type: "offer", sdp: "" }, init || {});
        };
        const FakeRTCIceCandidate = function(init) {
            return Object.assign({ candidate: "", sdpMid: null, sdpMLineIndex: null }, init || {});
        };
        const FakeRTCPeerConnection = function() {
            safePost({ type: "webrtc", detail: "RTCPeerConnection", blocked: true });
            this.connectionState = "closed";
            this.iceConnectionState = "closed";
            this.iceGatheringState = "complete";
            this.localDescription = null;
            this.remoteDescription = null;
            this.signalingState = "stable";
            this.onicecandidate = null;
            this.oniceconnectionstatechange = null;
            this.onconnectionstatechange = null;
        };
        FakeRTCPeerConnection.prototype.createOffer = function() { return Promise.resolve(FakeRTCSessionDescription({ type: "offer", sdp: "" })); };
        FakeRTCPeerConnection.prototype.createAnswer = function() { return Promise.resolve(FakeRTCSessionDescription({ type: "answer", sdp: "" })); };
        FakeRTCPeerConnection.prototype.setLocalDescription = function(description) {
            this.localDescription = FakeRTCSessionDescription(description);
            const self = this;
            Promise.resolve().then(function() {
                if (typeof self.onicecandidate === "function") {
                    self.onicecandidate({ candidate: null });
                }
            });
            return Promise.resolve();
        };
        FakeRTCPeerConnection.prototype.setRemoteDescription = function(description) {
            this.remoteDescription = FakeRTCSessionDescription(description);
            return Promise.resolve();
        };
        FakeRTCPeerConnection.prototype.addIceCandidate = function(candidate) {
            safePost({ type: "webrtc", detail: "iceCandidate", blocked: true });
            return Promise.resolve(FakeRTCIceCandidate(candidate));
        };
        FakeRTCPeerConnection.prototype.createDataChannel = function(label) {
            safePost({ type: "webrtc", detail: "dataChannel:" + (label || ""), blocked: true });
            return Object.freeze({
                label: label || "",
                readyState: "closed",
                close: noop,
                send: function() { throwSecurity("RTCDataChannel blocked"); }
            });
        };
        FakeRTCPeerConnection.prototype.getStats = function() { return Promise.resolve(new Map()); };
        FakeRTCPeerConnection.prototype.close = noop;

        defineValue(window, "RTCPeerConnection", FakeRTCPeerConnection);
        defineValue(window, "webkitRTCPeerConnection", FakeRTCPeerConnection);
        defineValue(window, "RTCSessionDescription", FakeRTCSessionDescription);
        defineValue(window, "RTCIceCandidate", FakeRTCIceCandidate);
        defineValue(window, "RTCDataChannel", undefined);
        defineValue(window, "MediaStreamTrack", undefined);
    }

    if (nativeWebSocket) {
        const WebSocketWrapper = function(url, protocols) {
            const resolved = new URL(url, location.href);
            const allowed = !policy.blockWebSockets && (
                !policy.allowFirstPartyWebSockets || sameSite(resolved.hostname, location.hostname)
            );
            const socketId = Math.random().toString(36).slice(2);

            if (!allowed) {
                safePost({
                    type: "websocket",
                    id: socketId,
                    url: resolved.href,
                    host: resolved.hostname,
                    port: resolved.port ? Number(resolved.port) : (resolved.protocol === "wss:" ? 443 : 80),
                    state: "blocked",
                    blocked: true
                });
                throwSecurity("WebSocket blocked");
            }

            safePost({
                type: "websocket",
                id: socketId,
                url: resolved.href,
                host: resolved.hostname,
                port: resolved.port ? Number(resolved.port) : (resolved.protocol === "wss:" ? 443 : 80),
                state: "attempt",
                blocked: false
            });

            const socket = protocols ? new nativeWebSocket(url, protocols) : new nativeWebSocket(url);
            socket.addEventListener("open", function() {
                safePost({
                    type: "websocket",
                    id: socketId,
                    url: resolved.href,
                    host: resolved.hostname,
                    port: resolved.port ? Number(resolved.port) : (resolved.protocol === "wss:" ? 443 : 80),
                    state: "open",
                    blocked: false
                });
            });
            const closeReporter = function(state) {
                safePost({
                    type: "websocket",
                    id: socketId,
                    url: resolved.href,
                    host: resolved.hostname,
                    port: resolved.port ? Number(resolved.port) : (resolved.protocol === "wss:" ? 443 : 80),
                    state: state,
                    blocked: false
                });
            };
            socket.addEventListener("close", function() { closeReporter("close"); });
            socket.addEventListener("error", function() { closeReporter("error"); });
            return socket;
        };
        WebSocketWrapper.prototype = nativeWebSocket.prototype;
        defineValue(window, "WebSocket", WebSocketWrapper);
    }

    if (policy.blockEval) {
        window.eval = function() { throwSecurity("eval blocked"); };
        window.Function = function() { throwSecurity("Function constructor blocked"); };
        if (window.WebAssembly) {
            window.WebAssembly.compile = function() { return denyPromise("WebAssembly blocked"); };
            window.WebAssembly.instantiate = function() { return denyPromise("WebAssembly blocked"); };
        }
    }

    if (policy.webGlDisabled) {
        const originalGetContext = HTMLCanvasElement.prototype.getContext;
        HTMLCanvasElement.prototype.getContext = function(type) {
            if (typeof type === "string" && ["webgl", "webgl2", "experimental-webgl"].indexOf(type.toLowerCase()) >= 0) {
                return null;
            }
            return originalGetContext.apply(this, arguments);
        };
        defineValue(window, "WebGLRenderingContext", undefined);
        defineValue(window, "WebGL2RenderingContext", undefined);
    } else if (window.WebGLRenderingContext) {
        const originalGetParameter = WebGLRenderingContext.prototype.getParameter;
        WebGLRenderingContext.prototype.getParameter = function(parameter) {
            if (parameter === 0x9245) return config.gpuVendor;
            if (parameter === 0x9246) return config.gpuRenderer;
            return originalGetParameter.apply(this, arguments);
        };
    }

    if (window.CanvasRenderingContext2D) {
        const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
        const originalMeasureText = CanvasRenderingContext2D.prototype.measureText;
        CanvasRenderingContext2D.prototype.getImageData = function() {
            const imageData = originalGetImageData.apply(this, arguments);
            for (let index = 0; index < imageData.data.length; index += 4) {
                imageData.data[index] = imageData.data[index] ^ (config.noiseSeed % 7);
            }
            return imageData;
        };
        CanvasRenderingContext2D.prototype.measureText = function(text) {
            const metrics = originalMeasureText.apply(this, arguments);
            if (!strictFingerprinting) {
                return metrics;
            }
            const roundedWidth = Math.round(metrics.width * 2) / 2;
            return new Proxy(metrics, {
                get: function(target, property) {
                    if (property === "width") {
                        return roundedWidth;
                    }
                    return target[property];
                }
            });
        };
    }

    if (window.HTMLCanvasElement && HTMLCanvasElement.prototype.toDataURL) {
        const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
        HTMLCanvasElement.prototype.toDataURL = function() {
            const context = this.getContext && this.getContext("2d");
            if (context && this.width && this.height) {
                context.save();
                context.fillStyle = "rgba(" + (config.noiseSeed % 255) + ",0,0,0.004)";
                context.fillRect(0, 0, 1, 1);
                context.restore();
            }
            return originalToDataURL.apply(this, arguments);
        };
    }

    if (window.AudioBuffer && AudioBuffer.prototype.getChannelData) {
        const originalGetChannelData = AudioBuffer.prototype.getChannelData;
        AudioBuffer.prototype.getChannelData = function() {
            const channelData = originalGetChannelData.apply(this, arguments);
            for (let index = 0; index < channelData.length; index += 100) {
                channelData[index] = channelData[index] + ((config.noiseSeed % 13) * 0.0000001);
            }
            return channelData;
        };
    }

    if (window.AudioContext) {
        const originalCreateAnalyser = AudioContext.prototype.createAnalyser;
        AudioContext.prototype.createAnalyser = function() {
            const analyser = originalCreateAnalyser.apply(this, arguments);
            const originalGetFloatFrequencyData = analyser.getFloatFrequencyData;
            analyser.getFloatFrequencyData = function(array) {
                originalGetFloatFrequencyData.call(this, array);
                if (array && array.length) {
                    array[0] = array[0] + ((config.noiseSeed % 5) * 0.01);
                }
            };
            return analyser;
        };
    }

    if (document.fonts) {
        try {
            document.fonts.check = function() { return false; };
            document.fonts.load = function() { return Promise.resolve([]); };
        } catch (ignored) {}
    }

    if (strictFingerprinting) {
        defineValue(window, "FontFace", undefined);
    }

    const fontStyle = document.createElement("style");
    fontStyle.textContent = [
        "html, body, button, input, textarea, select {",
        "  font-family: sans-serif, Arial, Helvetica !important;",
        "}",
        "@font-face { font-family: 'AmnosBlocked'; src: local('Arial'); }"
    ].join("");
    (document.head || document.documentElement || document.body || document).appendChild(fontStyle);

    const blockedRels = {
        "dns-prefetch": true,
        "preconnect": true,
        "prefetch": true,
        "prerender": true,
        "modulepreload": true
    };
    const scrubLinks = function(root) {
        if (!root || !root.querySelectorAll) {
            return;
        }
        root.querySelectorAll("link[rel]").forEach(function(node) {
            const rel = (node.getAttribute("rel") || "").toLowerCase();
            if ((policy.blockDnsPrefetch || policy.blockPreconnect) && blockedRels[rel]) {
                node.remove();
            }
        });
    };

    scrubLinks(document);
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            mutation.addedNodes.forEach(function(node) {
                if (node.tagName === "SCRIPT" && policy.blockInlineScripts && !node.src) {
                    node.textContent = "";
                    node.remove();
                }
                if (node.tagName === "LINK") {
                    scrubLinks(node.parentNode || document);
                }
            });
        });
    });
    observer.observe(document.documentElement || document, {
        childList: true,
        subtree: true
    });
})();
