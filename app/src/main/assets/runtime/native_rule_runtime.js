(function () {
    // 这个运行时只负责把 xiaomaojs / drpy 规则转换成原生页面可以直接消费的统一 JSON 结构。
    function safeString(value) {
        return value === undefined || value === null ? "" : String(value);
    }

    function parseRule(source) {
        var rule = null;
        try {
            eval(source + "\n//# sourceURL=xiaomao_rule.js");
        } catch (error) {
            throw new Error("规则解析失败: " + error.message);
        }
        if (!rule) {
            throw new Error("未找到 rule 对象");
        }
        return rule;
    }

    function parseClasses(rule) {
        var names = safeString(rule.class_name).split("&").filter(Boolean);
        var urls = safeString(rule.class_url).split("&").filter(Boolean);
        var list = [];
        for (var i = 0; i < Math.min(names.length, urls.length); i += 1) {
            list.push({
                name: names[i],
                typeId: urls[i]
            });
        }
        return list;
    }

    function buildCategoryUrl(rule, typeId, page) {
        return safeString(rule.url)
            .replace(/fyclass/g, typeId)
            .replace(/fypage/g, String(page || 1))
            .replace(/fyfilter/g, "");
    }

    function buildSearchUrl(rule, keyword, page) {
        var template = safeString(rule.searchUrl || rule.search_url || "");
        return template
            .replace(/\*\*/g, encodeURIComponent(keyword || ""))
            .replace(/fypage/g, String(page || 1));
    }

    function ensureAbsolute(rule, value) {
        var url = safeString(value);
        if (!url) return "";
        if (/^(https?:|magnet:|thunder:)/i.test(url)) {
            return url;
        }
        return urljoin(rule.host || "", url);
    }

    function normalizeList(result, rule) {
        if (!Array.isArray(result)) {
            return [];
        }
        return result.map(function (item) {
            return {
                title: safeString(item.title || item.vod_name),
                img: safeString(item.img || item.vod_pic),
                desc: safeString(item.desc || item.vod_remarks || item.vod_desc),
                url: ensureAbsolute(rule, item.url || item.vod_id)
            };
        }).filter(function (item) {
            return item.title && item.url;
        });
    }

    function normalizePlayGroups(detail) {
        var playFrom = safeString(detail.vod_play_from).split("$$$").filter(Boolean);
        var playUrls = safeString(detail.vod_play_url).split("$$$").filter(Boolean);
        var groups = [];

        for (var i = 0; i < Math.min(playFrom.length, playUrls.length); i += 1) {
            var name = playFrom[i] || ("线路" + (i + 1));
            var episodes = playUrls[i].split("#").filter(Boolean).map(function (episode) {
                var parts = episode.split("$");
                return {
                    name: safeString(parts[0] || "正片"),
                    url: safeString(parts.slice(1).join("$"))
                };
            }).filter(function (episode) {
                return episode.url;
            });
            if (episodes.length > 0) {
                groups.push({
                    name: name,
                    episodes: episodes
                });
            }
        }

        return groups;
    }

    function normalizeDetail(detail) {
        detail = detail || {};
        return {
            title: safeString(detail.vod_name),
            img: safeString(detail.vod_pic),
            remarks: safeString(detail.vod_remarks),
            year: safeString(detail.vod_year),
            area: safeString(detail.vod_area),
            director: safeString(detail.vod_director),
            actor: safeString(detail.vod_actor),
            content: safeString(detail.vod_content),
            playGroups: normalizePlayGroups(detail)
        };
    }

    function normalizePlay(play) {
        if (typeof play === "string") {
            return {
                url: play,
                parse: false,
                jx: 0,
                headers: {}
            };
        }
        play = play || {};
        return {
            url: safeString(play.url || play.input || ""),
            parse: !!play.parse,
            jx: Number(play.jx || 0),
            headers: play.header || play.headers || {}
        };
    }

    function resetEnv(rule, input, page) {
        window.rule = rule;
        window.input = input;
        window.MY_PAGE = page || 1;
        window.MY_PAGECOUNT = 999;
        window.MY_TOTAL = 9999;
        window.MY_FL = {};
        window.VOD = null;
        window.__drpyResult = null;
    }

    function setupHelpers() {
        window.setResult = function (value) {
            window.__drpyResult = value;
            return value;
        };
        window.request = function (url, options) {
            return AndroidBridge.request(JSON.stringify({
                url: url,
                options: options || {}
            }));
        };
        window.post = function (url, body, options) {
            var merged = options || {};
            merged.method = "POST";
            merged.body = body || "";
            return request(url, merged);
        };
        window.urljoin = function (base, relative) {
            return AndroidBridge.joinUrl(safeString(base), safeString(relative));
        };
        window.base64Encode = function (text) {
            return AndroidBridge.base64Encode(safeString(text));
        };
        window.base64Decode = function (text) {
            return AndroidBridge.base64Decode(safeString(text));
        };
        window.log = function () {
            AndroidBridge.log(Array.prototype.slice.call(arguments).join(" "));
        };
    }

    function runBlock(block, rule, input, page) {
        resetEnv(rule, input, page);
        setupHelpers();

        if (typeof block === "function") {
            block();
        } else if (typeof block === "string") {
            var code = block.indexOf("js:") === 0 ? block.substring(3) : block;
            eval(code);
        } else {
            return block;
        }

        if (window.__drpyResult !== null && window.__drpyResult !== undefined) {
            return window.__drpyResult;
        }
        if (window.VOD) {
            return window.VOD;
        }
        return null;
    }

    window.__nativeRuleRuntime = {
        execute: function (payloadJson) {
            try {
                var payload = JSON.parse(payloadJson);
                var rule = parseRule(payload.source);
                var action = safeString(payload.action);
                var page = Number(payload.page || 1);
                var data = {};

                if (action === "home") {
                    data.meta = {
                        title: safeString(rule.title || rule.host),
                        host: safeString(rule.host),
                        searchable: !!rule.searchable,
                        categories: parseClasses(rule)
                    };
                    data.list = normalizeList(runBlock(rule["推荐"], rule, rule.host, 1), rule);
                } else if (action === "category") {
                    data.list = normalizeList(
                        runBlock(rule["一级"], rule, buildCategoryUrl(rule, payload.input, page), page),
                        rule
                    );
                } else if (action === "search") {
                    data.list = normalizeList(
                        runBlock(rule["搜索"], rule, buildSearchUrl(rule, payload.input, page), page),
                        rule
                    );
                } else if (action === "detail") {
                    data.detail = normalizeDetail(runBlock(rule["二级"], rule, ensureAbsolute(rule, payload.input), 1));
                } else if (action === "lazy") {
                    data.play = normalizePlay(runBlock(rule["lazy"], rule, payload.input, 1));
                } else if (action === "meta") {
                    data.meta = {
                        title: safeString(rule.title || rule.host),
                        host: safeString(rule.host),
                        searchable: !!rule.searchable,
                        categories: parseClasses(rule)
                    };
                } else {
                    throw new Error("不支持的动作: " + action);
                }

                return JSON.stringify({
                    ok: true,
                    data: data
                });
            } catch (error) {
                return JSON.stringify({
                    ok: false,
                    error: error && error.message ? error.message : String(error)
                });
            }
        }
    };
})();
