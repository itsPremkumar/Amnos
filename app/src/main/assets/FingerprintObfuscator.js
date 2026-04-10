(function() {
    const config = window._privacyConfig || {};

    // 1. Global Privacy Control (GPC)
    Object.defineProperty(navigator, 'globalPrivacyControl', {
        value: true, writable: false, configurable: false
    });

    // 2. Coherent Navigator Spoofing
    const proxyNavigator = new Proxy(navigator, {
        get: (target, prop) => {
            if (prop === 'userAgent') return config.userAgent;
            if (prop === 'platform') return config.platform;
            if (prop === 'language') return config.languages[0];
            if (prop === 'languages') return config.languages;
            if (prop === 'hardwareConcurrency') return config.hardwareConcurrency;
            if (prop === 'deviceMemory') return config.deviceMemory;
            if (prop === 'webdriver') return false;
            
            let val = target[prop];
            if (typeof val === 'function') return val.bind(target);
            return val;
        }
    });

    // 3. Geolocation Protection (Silent Block)
    if (navigator.geolocation) {
        const geoError = (errorCallback) => {
            if (errorCallback) {
                errorCallback({
                    code: 1,
                    message: "User denied Geolocation",
                    PERMISSION_DENIED: 1,
                    POSITION_UNAVAILABLE: 2,
                    TIMEOUT: 3
                });
            }
        };
        navigator.geolocation.getCurrentPosition = (success, error) => geoError(error);
        navigator.geolocation.watchPosition = (success, error) => geoError(error);
    }

    // 4. WebGL Fingerprinting Protection (GPU Spoofing)
    const originalGetParameter = WebGLRenderingContext.prototype.getParameter;
    WebGLRenderingContext.prototype.getParameter = function(parameter) {
        // UNMASKED_VENDOR_WEBGL = 0x9245, UNMASKED_RENDERER_WEBGL = 0x9246
        if (parameter === 0x9245) return config.gpuVendor;
        if (parameter === 0x9246) return config.gpuRenderer;
        return originalGetParameter.apply(this, arguments);
    };

    const originalGetExtension = WebGLRenderingContext.prototype.getExtension;
    WebGLRenderingContext.prototype.getExtension = function(name) {
        if (name === 'WEBGL_debug_renderer_info') return {
            UNMASKED_VENDOR_WEBGL: 0x9245,
            UNMASKED_RENDERER_WEBGL: 0x9246
        };
        return originalGetExtension.apply(this, arguments);
    };

    // 5. Canvas Protection (Noise)
    const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
    CanvasRenderingContext2D.prototype.getImageData = function() {
        const imageData = originalGetImageData.apply(this, arguments);
        for (let i = 0; i < imageData.data.length; i += 4) {
            imageData.data[i] = imageData.data[i] ^ (config.noiseSeed % 3);
        }
        return imageData;
    };

    // 6. Audio Protection (Noise)
    const originalGetChannelData = AudioBuffer.prototype.getChannelData;
    AudioBuffer.prototype.getChannelData = function() {
        const data = originalGetChannelData.apply(this, arguments);
        for (let i = 0; i < data.length; i += 100) {
            data[i] = data[i] + (0.0000001 * (config.noiseSeed % 10));
        }
        return data;
    };

    // 7. Battery Status API Protection
    if (navigator.getBattery) {
        navigator.getBattery = () => Promise.resolve({
            level: 0.76, charging: true, chargingTime: 0, dischargingTime: Infinity,
            addEventListener: () => {}, removeEventListener: () => {},
            onlevelchange: null, onchargingchange: null, onchargingtimechange: null, ondischargingtimechange: null
        });
    }

    // 8. Screen & UI Resolution Spoofing
    const proxyScreen = new Proxy(screen, {
        get: (target, prop) => {
            if (config.screen[prop]) return config.screen[prop];
            let val = target[prop];
            if (typeof val === 'function') return val.bind(target);
            return val;
        }
    });
    
    // Inject proxies
    Object.defineProperty(window, 'navigator', { value: proxyNavigator, writable: false });
    Object.defineProperty(window, 'screen', { value: proxyScreen, writable: false });
    Object.defineProperty(window, 'devicePixelRatio', { value: config.screen.devicePixelRatio, writable: false });

    // 9. Clipboard & Font Protections (v2)
    if (navigator.clipboard) {
        const block = () => Promise.reject(new Error("Blocked"));
        Object.defineProperty(navigator, 'clipboard', { 
            value: { readText: block, read: block, writeText: block, write: block }, 
            writable: false 
        });
    }

    const style = document.createElement('style');
    style.innerHTML = `* { font-family: sans-serif !important; }`;
    document.documentElement.appendChild(style);

    console.log("Modular Identity Engine Active: Profile=" + config.userAgent);
})();
