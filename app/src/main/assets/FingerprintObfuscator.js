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

    const navigatorProto = Object.getPrototypeOf(navigator);
    const screenProto = Object.getPrototypeOf(screen);

    defineGetter(navigatorProto, "userAgent", function() { return config.userAgent; });
    defineGetter(navigatorProto, "platform", function() { return config.platform; });
    defineGetter(navigatorProto, "language", function() { return config.languages[0]; });
    defineGetter(navigatorProto, "languages", function() { return config.languages.slice(); });
    defineGetter(navigatorProto, "hardwareConcurrency", function() { return config.hardwareConcurrency; });
    defineGetter(navigatorProto, "deviceMemory", function() { return config.deviceMemory; });
    defineGetter(navigatorProto, "webdriver", function() { return false; });
    defineGetter(navigatorProto, "doNotTrack", function() { return "1"; });
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
        const blockedRtc = function() { throwSecurity("WebRTC blocked"); };
        defineValue(window, "RTCPeerConnection", blockedRtc);
        defineValue(window, "webkitRTCPeerConnection", blockedRtc);
        defineValue(window, "RTCDataChannel", undefined);
        defineValue(window, "MediaStreamTrack", undefined);
    }

    if (policy.blockWebSockets) {
        const blockedWebSocket = function() { throwSecurity("WebSocket blocked"); };
        defineValue(window, "WebSocket", blockedWebSocket);
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
        CanvasRenderingContext2D.prototype.getImageData = function() {
            const imageData = originalGetImageData.apply(this, arguments);
            for (let index = 0; index < imageData.data.length; index += 4) {
                imageData.data[index] = imageData.data[index] ^ (config.noiseSeed % 7);
            }
            return imageData;
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
