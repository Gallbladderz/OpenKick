package com.gallbladderz.openkick.core

object WebViewScripts {
    val CLOUDFLARE_BYPASS_SCRIPT = """
        (function() {
            if (window.kickBypassInjected) return;
            window.kickBypassInjected = true;

            function tryFetch() {
                if (document.title.includes("Just a moment") || document.title.includes("Cloudflare")) {
                    setTimeout(tryFetch, 2000);
                    return;
                }

                fetch('https://web.kick.com/api/v1/livestreams/featured?language=ru', {
                    headers: {
                        'Accept': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                })
                .then(async response => {
                    const text = await response.text();
                    const trimmed = text.trim();
                    
                    if (response.ok && (trimmed.startsWith("[") || trimmed.startsWith("{"))) {
                        window.AndroidBridge.sendDataToAndroid(trimmed);
                    } 
                    else if (response.status === 403 || trimmed.startsWith("<") || trimmed === "") {
                        setTimeout(tryFetch, 2000);
                    } 
                    else {
                        window.AndroidBridge.sendDataToAndroid("JS_ERROR: " + response.status + " " + trimmed.substring(0, 50));
                    }
                })
                .catch(err => {
                    setTimeout(tryFetch, 2000); 
                });
            }
            setTimeout(tryFetch, 1000);
        })();
    """.trimIndent()

    val CATEGORIES_BYPASS_SCRIPT = """
        (function() {
            if (window.categoriesBypassInjected) return;
            window.categoriesBypassInjected = true;

            function tryFetch() {
                if (document.title.includes("Just a moment") || document.title.includes("Cloudflare")) {
                    setTimeout(tryFetch, 1000);
                    return;
                }

                fetch('https://kick.com/api/v1/subcategories?limit=100', {
                    headers: { 'Accept': 'application/json' }
                })
                .then(async response => {
                    const text = await response.text();
                    const trimmed = text.trim();
                    if (response.ok) {
                        window.AndroidBridge.sendDataToAndroid(trimmed);
                    } else {
                        setTimeout(tryFetch, 1000); 
                    }
                })
                .catch(err => {
                    setTimeout(tryFetch, 1000); 
                });
            }
            setTimeout(tryFetch, 500);
        })();
    """.trimIndent()

    fun getChannelBypassScript(streamerName: String): String = """
        (function() {
            if (window.kickPlayerBypassInjected) return;
            window.kickPlayerBypassInjected = true;

            function tryFetch() {
                if (document.title.includes("Just a moment") || document.title.includes("Cloudflare")) {
                    setTimeout(tryFetch, 300);
                    return;
                }

                fetch('https://kick.com/api/v2/channels/$streamerName', {
                    headers: { 'Accept': 'application/json' }
                })
                .then(async response => {
                    const text = await response.text();
                    if (response.ok) {
                        window.AndroidBridge.sendDataToAndroid(text);
                    } else {
                        setTimeout(tryFetch, 500);
                    }
                })
                .catch(err => {
                    setTimeout(tryFetch, 500);
                });
            }
            tryFetch();
        })();
    """.trimIndent()

    val SEARCH_BYPASS_SCRIPT = """
        (function() {
            if (window.searchInjected) return;
            window.searchInjected = true;

            window.doSearch = function(query) {
                if (!query) {
                    window.AndroidBridge.sendDataToAndroid('{"empty":true}');
                    return;
                }
                fetch('https://kick.com/api/search?searched_word=' + encodeURIComponent(query), {
                    headers: { 'Accept': 'application/json' }
                })
                .then(async response => {
                    const text = await response.text();
                    if (response.ok) {
                        window.AndroidBridge.sendDataToAndroid(text);
                    } else {
                        window.AndroidBridge.sendDataToAndroid("JS_ERROR: " + response.status);
                    }
                })
                .catch(err => {
                    window.AndroidBridge.sendDataToAndroid("JS_ERROR: fetch failed");
                });
            };
        })();
    """.trimIndent()
}
