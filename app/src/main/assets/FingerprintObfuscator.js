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
    
    // Dynamic search engine detection to handle in-tab navigations
    const searchHosts = ["duckduckgo.com", "google.com", "www.google.com", "www.google.co.in"];
    const mediaCompatibilityHosts = [
        "youtube.com",
        "www.youtube.com",
        "youtube-nocookie.com",
        "youtu.be",
        "vimeo.com",
        "www.vimeo.com",
        "player.vimeo.com",
        "vimeocdn.com"
    ];
    const isSearchEngine = searchHosts.some(h => window.location.hostname.endsWith(h));
    const isCompatibilityCriticalHost = isSearchEngine || mediaCompatibilityHosts.some(h => window.location.hostname.endsWith(h));
    const noop = function() {};
    const nativeWebSocket = window.WebSocket;
    const nativeDateNow = Date.now.bind(Date);
    const nativeRequestAnimationFrame = window.requestAnimationFrame ? window.requestAnimationFrame.bind(window) : null;
    const nativePerformanceNow = window.performance && window.performance.now ? window.performance.now.bind(window.performance) : null;
    const strictFingerprinting = policy.fingerprintLevel === "STRICT";
    const fingerprintEnabled = policy.fingerprintLevel !== "DISABLED";

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
    const lastReported = new Map();
    const safePost = function(payload) {
        try {
            if (payload.type === "spoof") {
                const key = payload.detail;
                const now = nativeDateNow();
                if (lastReported.has(key) && (now - lastReported.get(key) < 2000)) {
                    return;
                }
                lastReported.set(key, now);
            }

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
        const multipartSuffixes = [
            "co.uk", "org.uk", "gov.uk", "ac.uk",
            "co.in", "com.au", "net.au", "org.au",
            "co.jp", "com.br", "com.mx", "co.nz"
        ];
        const suffix = labels.slice(-2).join(".");
        if (labels.length >= 3 && multipartSuffixes.indexOf(suffix) >= 0) {
            return labels.slice(-3).join(".");
        }
        return suffix;
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

    if (fingerprintEnabled) {
        defineGetter(navigatorProto, "userAgent", function() { 
            safePost({ type: "spoof", detail: "navigator.userAgent" });
            return config.userAgent; 
        });
        defineGetter(navigatorProto, "appVersion", function() { 
            return config.userAgent.replace(/^Mozilla\//, ""); 
        });
        defineGetter(navigatorProto, "vendor", function() { return "Google Inc."; });
        defineGetter(navigatorProto, "vendorSub", function() { return ""; });
        defineGetter(navigatorProto, "productSub", function() { return "20030107"; });
        defineGetter(navigatorProto, "platform", function() { return config.platform; });
        defineGetter(navigatorProto, "oscpu", function() { return undefined; });
        defineGetter(navigatorProto, "buildID", function() { return undefined; });
        defineGetter(navigatorProto, "language", function() { return config.languages[0]; });
        defineGetter(navigatorProto, "languages", function() { return freezeList(config.languages); });
        defineGetter(navigatorProto, "hardwareConcurrency", function() { return config.hardwareConcurrency; });
        defineGetter(navigatorProto, "deviceMemory", function() { return config.deviceMemory; });
        defineGetter(navigatorProto, "maxTouchPoints", function() { return 5; });
        defineGetter(navigatorProto, "webdriver", function() { return false; });
        defineGetter(navigatorProto, "doNotTrack", function() { return "1"; });
        defineGetter(navigatorProto, "plugins", function() { return freezeList([]); });
        defineGetter(navigatorProto, "mimeTypes", function() { return freezeList([]); });
        defineValue(navigator, "globalPrivacyControl", true);

        // Additional Navigator Properties
        defineGetter(navigatorProto, "appCodeName", function() { return "Mozilla"; });
        defineGetter(navigatorProto, "appName", function() { return "Netscape"; });
        defineGetter(navigatorProto, "product", function() { return "Gecko"; });
        defineGetter(navigatorProto, "pdfViewerEnabled", function() { return true; });
        defineGetter(navigatorProto, "cookieEnabled", function() { return false; });
        defineGetter(navigatorProto, "onLine", function() { return true; });
    }
    
    if (fingerprintEnabled) {
        // USB/Bluetooth/HID Device Enumeration Blocking
        if (navigator.usb) {
            defineValue(navigator, "usb", Object.freeze({
                getDevices: function() { return Promise.resolve([]); },
                requestDevice: function() { return denyPromise("USB blocked"); },
                addEventListener: noop,
                removeEventListener: noop
            }));
        }
        
        if (navigator.bluetooth) {
            defineValue(navigator, "bluetooth", Object.freeze({
                getAvailability: function() { return Promise.resolve(false); },
                getDevices: function() { return Promise.resolve([]); },
                requestDevice: function() { return denyPromise("Bluetooth blocked"); },
                addEventListener: noop,
                removeEventListener: noop
            }));
        }
        
        if (navigator.hid) {
            defineValue(navigator, "hid", Object.freeze({
                getDevices: function() { return Promise.resolve([]); },
                requestDevice: function() { return denyPromise("HID blocked"); },
                addEventListener: noop,
                removeEventListener: noop
            }));
        }
        
        if (navigator.serial) {
            defineValue(navigator, "serial", Object.freeze({
                getPorts: function() { return Promise.resolve([]); },
                requestPort: function() { return denyPromise("Serial blocked"); },
                addEventListener: noop,
                removeEventListener: noop
            }));
        }

        // MIDI Device Enumeration Blocking
        if (navigator.requestMIDIAccess) {
            navigator.requestMIDIAccess = function() {
                return denyPromise("MIDI blocked");
            };
        }

        // Presentation API Blocking
        if (navigator.presentation) {
            defineValue(navigator, "presentation", Object.freeze({
                defaultRequest: null,
                receiver: null
            }));
        }

        // XR (VR/AR) API Blocking
        if (navigator.xr) {
            defineValue(navigator, "xr", Object.freeze({
                isSessionSupported: function() { return Promise.resolve(false); },
                requestSession: function() { return denyPromise("XR blocked"); },
                addEventListener: noop,
                removeEventListener: noop
            }));
        }
    }

    if (fingerprintEnabled) {
        defineGetter(screenProto, "width", function() { 
            safePost({ type: "spoof", detail: "screen.width" });
            return config.screen.width; 
        });
        defineGetter(screenProto, "height", function() { 
            safePost({ type: "spoof", detail: "screen.height" });
            return config.screen.height; 
        });
        defineGetter(screenProto, "availWidth", function() { return config.screen.availWidth; });
        defineGetter(screenProto, "availHeight", function() { return config.screen.availHeight; });
        defineGetter(screenProto, "colorDepth", function() { return config.screen.colorDepth; });
        defineGetter(screenProto, "pixelDepth", function() { return config.screen.pixelDepth; });
        defineGetter(window, "devicePixelRatio", function() { return config.screen.devicePixelRatio; });
        defineGetter(window, "outerWidth", function() { return config.screen.width; });
        defineGetter(window, "outerHeight", function() { return config.screen.height; });
        defineGetter(window, "innerWidth", function() { return config.screen.availWidth; });
        defineGetter(window, "innerHeight", function() { return config.screen.availHeight; });
        defineGetter(window, "screenX", function() { return 0; });
        defineGetter(window, "screenY", function() { return 0; });
        defineGetter(window, "screenLeft", function() { return 0; });
        defineGetter(window, "screenTop", function() { return 0; });

        if (screen.orientation) {
            const orientationProto = Object.getPrototypeOf(screen.orientation);
            defineGetter(orientationProto, "type", function() { return "portrait-primary"; });
            defineGetter(orientationProto, "angle", function() { return 0; });
        }
    }

    if (fingerprintEnabled) {
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
    }

    if (fingerprintEnabled) {
        if (window.performance && nativePerformanceNow) {
            window.performance.now = function() {
                return quantizeTime(nativePerformanceNow());
            };

            // Performance Timeline API - can leak resource timing
            if (window.performance.getEntries) {
                window.performance.getEntries = function() { return []; };
                window.performance.getEntriesByType = function() { return []; };
                window.performance.getEntriesByName = function() { return []; };
            }

            // Performance Observer - can track resource loads
            if (window.PerformanceObserver) {
                const OriginalPerformanceObserver = window.PerformanceObserver;
                window.PerformanceObserver = function(callback) {
                    return new OriginalPerformanceObserver(function(list, observer) {
                        // Filter out resource timing entries
                        const filteredList = {
                            getEntries: function() { return []; },
                            getEntriesByType: function() { return []; },
                            getEntriesByName: function() { return []; }
                        };
                        callback(filteredList, observer);
                    });
                };
                window.PerformanceObserver.supportedEntryTypes = [];
            }

            // Navigation Timing - can leak page load info
            if (window.performance.timing) {
                const fakeTimingBase = nativeDateNow();
                defineGetter(window.performance, "timing", function() {
                    return Object.freeze({
                        navigationStart: fakeTimingBase,
                        unloadEventStart: 0,
                        unloadEventEnd: 0,
                        redirectStart: 0,
                        redirectEnd: 0,
                        fetchStart: fakeTimingBase,
                        domainLookupStart: fakeTimingBase,
                        domainLookupEnd: fakeTimingBase,
                        connectStart: fakeTimingBase,
                        connectEnd: fakeTimingBase,
                        secureConnectionStart: fakeTimingBase,
                        requestStart: fakeTimingBase,
                        responseStart: fakeTimingBase,
                        responseEnd: fakeTimingBase,
                        domLoading: fakeTimingBase,
                        domInteractive: fakeTimingBase,
                        domContentLoadedEventStart: fakeTimingBase,
                        domContentLoadedEventEnd: fakeTimingBase,
                        domComplete: fakeTimingBase,
                        loadEventStart: fakeTimingBase,
                        loadEventEnd: fakeTimingBase
                    });
                });
            }

            // Memory Info - can leak device capabilities
            if (window.performance.memory) {
                defineGetter(window.performance, "memory", function() {
                    return Object.freeze({
                        jsHeapSizeLimit: 2172649472,
                        totalJSHeapSize: 1500000000,
                        usedJSHeapSize: 1000000000
                    });
                });
            }
        }

        if (nativeRequestAnimationFrame) {
            window.requestAnimationFrame = function(callback) {
                return nativeRequestAnimationFrame(function(timestamp) {
                    callback(quantizeTime(timestamp));
                });
            };
        }
    }

    if (fingerprintEnabled) {
        const createNoopStorage = (name) => {
            const data = new Map();
            return {
                getItem: function(key) { return data.get(String(key)) || null; },
                setItem: function(key, value) { 
                    try {
                        data.set(String(key), String(value));
                        if (data.size > 100) { data.delete(data.keys().next().value); }
                    } catch(e) {}
                },
                removeItem: function(key) { data.delete(String(key)); },
                clear: function() { data.clear(); },
                key: function(i) { return Array.from(data.keys())[i] || null; },
                get length() { return data.size; }
            };
        };

        const blockedLocalStorage = createNoopStorage("localStorage");
        const blockedSessionStorage = createNoopStorage("sessionStorage");
        
        defineGetter(window, "localStorage", function() { return blockedLocalStorage; });
        defineGetter(window, "sessionStorage", function() { return blockedSessionStorage; });
        defineValue(window, "openDatabase", undefined);
        defineGetter(window, "indexedDB", function() { return undefined; });
        defineValue(window, "caches", Object.freeze({
            open: function() { return denyPromise("Cache API disabled"); },
            match: function() { return Promise.resolve(undefined); },
            delete: function() { return Promise.resolve(false); },
            keys: function() { return Promise.resolve([]); }
        }));
    }

    if (fingerprintEnabled) {
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
                    "display-capture",
                    "midi",
                    "notifications",
                    "nfc",
                    "persistent-storage",
                    "speaker-selection",
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
        defineValue(window, "LinearAccelerationSensor", undefined);
        defineValue(window, "GravitySensor", undefined);

        // Ambient Light Sensor Blocking
        defineValue(window, "AmbientLightSensor", undefined);
        
        // Proximity Sensor Blocking
        if (window.ProximitySensor) {
            defineValue(window, "ProximitySensor", undefined);
        }

        // Vibration API Blocking
        if (navigator.vibrate) {
            navigator.vibrate = function() { return false; };
        }

        // Wake Lock API Blocking
        if (navigator.wakeLock) {
            defineValue(navigator, "wakeLock", Object.freeze({
                request: function() { return denyPromise("Wake lock blocked"); }
            }));
        }

        // Screen Wake Lock (older API)
        if (screen.keepAwake !== undefined) {
            defineGetter(screen, "keepAwake", function() { return false; });
        }

        // Idle Detection API Blocking
        if (window.IdleDetector) {
            defineValue(window, "IdleDetector", undefined);
        }

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
                safePost({ type: "spoof", detail: "navigator.getBattery" });
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
            defineGetter(connectionProto, "type", function() { return "wifi"; });
            defineGetter(connectionProto, "downlinkMax", function() { return Infinity; });
        }

        // Network Information API v2 (experimental)
        if (navigator.mozConnection) {
            defineValue(navigator, "mozConnection", navigator.connection);
        }
        if (navigator.webkitConnection) {
            defineValue(navigator, "webkitConnection", navigator.connection);
        }
    }

    // Beacon API - can be used for tracking
    if (navigator.sendBeacon) {
        const originalSendBeacon = navigator.sendBeacon.bind(navigator);
        navigator.sendBeacon = function(url, data) {
            safePost({ type: "beacon", url: url, blocked: policy.blockTrackers });
            if (policy.blockTrackers) {
                return false;
            }
            return originalSendBeacon(url, data);
        };
    }

    if (navigator.getGamepads) {
        navigator.getGamepads = function() { return []; };
    }

    if (navigator.keyboard) {
        defineValue(navigator, "keyboard", Object.freeze({
            getLayoutMap: function() { return Promise.resolve(new Map()); },
            lock: function() { return denyPromise("Keyboard lock blocked"); },
            unlock: noop
        }));
    }

    if (navigator.mediaCapabilities) {
        const originalDecodingInfo = navigator.mediaCapabilities.decodingInfo;
        navigator.mediaCapabilities.decodingInfo = function(config) {
            if (originalDecodingInfo) {
                return originalDecodingInfo.call(navigator.mediaCapabilities, config);
            }
            return Promise.resolve({
                supported: true,
                smooth: true,
                powerEfficient: true
            });
        };
    }

    if (window.speechSynthesis && speechSynthesis.getVoices) {
        const originalGetVoices = speechSynthesis.getVoices.bind(speechSynthesis);
        speechSynthesis.getVoices = function() {
            const voices = originalGetVoices();
            return voices.length > 0 ? [voices[0]] : [];
        };
    }

    if (navigator.scheduling) {
        defineValue(navigator, "scheduling", Object.freeze({
            isInputPending: function() { return false; }
        }));
    }

    if (policy.blockServiceWorkers && !isCompatibilityCriticalHost && navigator.serviceWorker) {
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

    if (policy.blockEval && !isCompatibilityCriticalHost) {
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
    } else {
        if (window.WebGLRenderingContext) {
            const originalGetParameter = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = function(parameter) {
                if (parameter === 0x9245) {
                    safePost({ type: "spoof", detail: "WebGL GPU Vendor" });
                    return config.gpuVendor;
                }
                if (parameter === 0x9246) {
                    safePost({ type: "spoof", detail: "WebGL GPU Renderer" });
                    return config.gpuRenderer;
                }
                return originalGetParameter.apply(this, arguments);
            };
        }
        if (window.WebGL2RenderingContext) {
            const originalGetParameter2 = WebGL2RenderingContext.prototype.getParameter;
            WebGL2RenderingContext.prototype.getParameter = function(parameter) {
                if (parameter === 0x9245) return config.gpuVendor;
                if (parameter === 0x9246) return config.gpuRenderer;
                return originalGetParameter2.apply(this, arguments);
            };
        }
    }

    if (window.CanvasRenderingContext2D) {
        const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
        const originalMeasureText = CanvasRenderingContext2D.prototype.measureText;
        CanvasRenderingContext2D.prototype.getImageData = function() {
            safePost({ type: "spoof", detail: "Canvas.getImageData (Noise Injection)" });
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

        // toBlob also leaks canvas fingerprint
        const originalToBlob = HTMLCanvasElement.prototype.toBlob;
        HTMLCanvasElement.prototype.toBlob = function(callback) {
            const context = this.getContext && this.getContext("2d");
            if (context && this.width && this.height) {
                context.save();
                context.fillStyle = "rgba(" + (config.noiseSeed % 255) + ",0,0,0.004)";
                context.fillRect(0, 0, 1, 1);
                context.restore();
            }
            return originalToBlob.apply(this, arguments);
        };
    }

    // OffscreenCanvas Fingerprinting Protection
    if (window.OffscreenCanvas) {
        const OriginalOffscreenCanvas = window.OffscreenCanvas;
        window.OffscreenCanvas = function(width, height) {
            const canvas = new OriginalOffscreenCanvas(width, height);
            const originalConvertToBlob = canvas.convertToBlob;
            if (originalConvertToBlob) {
                canvas.convertToBlob = function() {
                    const context = canvas.getContext && canvas.getContext("2d");
                    if (context && canvas.width && canvas.height) {
                        context.save();
                        context.fillStyle = "rgba(" + (config.noiseSeed % 255) + ",0,0,0.004)";
                        context.fillRect(0, 0, 1, 1);
                        context.restore();
                    }
                    return originalConvertToBlob.apply(canvas, arguments);
                };
            }
            return canvas;
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

        // Additional AudioContext fingerprinting vectors
        const OriginalAudioContext = window.AudioContext;
        window.AudioContext = function() {
            const ctx = new OriginalAudioContext();
            
            // Spoof baseLatency (can reveal hardware)
            if (ctx.baseLatency !== undefined) {
                defineGetter(ctx, "baseLatency", function() { return 0.01; });
            }
            
            // Spoof outputLatency (can reveal hardware)
            if (ctx.outputLatency !== undefined) {
                defineGetter(ctx, "outputLatency", function() { return 0.02; });
            }
            
            return ctx;
        };
    }

    // OfflineAudioContext Fingerprinting Protection
    if (window.OfflineAudioContext) {
        const OriginalOfflineAudioContext = window.OfflineAudioContext;
        window.OfflineAudioContext = function(numberOfChannels, length, sampleRate) {
            const ctx = new OriginalOfflineAudioContext(numberOfChannels, length, sampleRate);
            const originalStartRendering = ctx.startRendering;
            ctx.startRendering = function() {
                return originalStartRendering.call(ctx).then(function(buffer) {
                    // Add noise to offline audio rendering
                    for (let channel = 0; channel < buffer.numberOfChannels; channel++) {
                        const channelData = buffer.getChannelData(channel);
                        for (let i = 0; i < channelData.length; i += 100) {
                            channelData[i] = channelData[i] + ((config.noiseSeed % 13) * 0.0000001);
                        }
                    }
                    return buffer;
                });
            };
            return ctx;
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

    // CSS Media Query Fingerprinting Protection
    if (window.matchMedia) {
        const originalMatchMedia = window.matchMedia.bind(window);
        window.matchMedia = function(query) {
            const result = originalMatchMedia(query);
            const queryLower = query.toLowerCase();
            let spoofedMatches = null;

            if (queryLower.includes("prefers-color-scheme")) {
                spoofedMatches = queryLower.includes("light");
            } else if (queryLower.includes("prefers-reduced-motion")) {
                spoofedMatches = false;
            } else if (queryLower.includes("prefers-contrast")) {
                spoofedMatches = queryLower.includes("no-preference");
            } else if (queryLower.includes("inverted-colors")) {
                spoofedMatches = false;
            }

            if (spoofedMatches !== null) {
                return new Proxy(result, {
                    get: function(target, prop) {
                        if (prop === "matches") {
                            return spoofedMatches;
                        }
                        const value = target[prop];
                        if (typeof value === "function") {
                            return value.bind(target);
                        }
                        return value;
                    }
                });
            }
            return result;
        };
    }

    // Pointer Events API Spoofing
    if (window.PointerEvent) {
        const originalPointerEvent = window.PointerEvent;
        window.PointerEvent = function(type, init) {
            if (init) {
                init.pressure = init.pressure !== undefined ? 0.5 : init.pressure;
                init.tangentialPressure = 0;
                init.tiltX = 0;
                init.tiltY = 0;
                init.twist = 0;
            }
            return new originalPointerEvent(type, init);
        };
    }

    // Touch Event Fingerprinting Protection
    if (window.Touch && Touch.prototype) {
        const originalTouch = window.Touch;
        window.Touch = function(init) {
            if (init) {
                init.force = init.force !== undefined ? 0.5 : init.force;
                init.rotationAngle = 0;
                init.radiusX = init.radiusX !== undefined ? 20 : init.radiusX;
                init.radiusY = init.radiusY !== undefined ? 20 : init.radiusY;
            }
            return new originalTouch(init);
        };
    }

    // Error Stack Trace Fingerprinting Protection
    const originalError = window.Error;
    window.Error = function(message) {
        const err = new originalError(message);
        if (strictFingerprinting && err.stack) {
            // Sanitize stack traces to remove file paths
            err.stack = err.stack.split('\n').map(function(line) {
                return line.replace(/https?:\/\/[^\s)]+/g, '<sanitized>');
            }).join('\n');
        }
        return err;
    };

    // Document.referrer spoofing (already handled by headers, but JS can still read it)
    if (policy.stripReferrers) {
        defineGetter(document, "referrer", function() { return ""; });
    }

    // Document.domain fingerprinting
    const originalDomain = document.domain;
    defineGetter(document, "domain", function() { return originalDomain; });

    // History length can leak browsing behavior
    if (strictFingerprinting) {
        defineGetter(window.history, "length", function() { return 2; });
    }

    // Notification API - can be used for fingerprinting
    if (window.Notification) {
        const OriginalNotification = window.Notification;
        defineGetter(OriginalNotification, "permission", function() { return "denied"; });
        OriginalNotification.requestPermission = function() {
            return Promise.resolve("denied");
        };
    }
})();
