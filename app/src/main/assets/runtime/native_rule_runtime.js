(function () {
  function safeString(value) {
    return value === undefined || value === null ? "" : String(value);
  }

  function isUrl(value) {
    return /^(https?:)?\/\//i.test(safeString(value));
  }

  function ensureAbsolute(base, value) {
    const url = safeString(value);
    if (!url) return "";
    if (/^(https?:|magnet:|thunder:|ws:|ftp:)/i.test(url)) {
      return url;
    }
    return AndroidBridge.joinUrl(safeString(base), url);
  }

  function requestText(url, options) {
    return AndroidBridge.request(
      JSON.stringify({
        url,
        options: options || {},
      }),
    );
  }

  function parseSourcePackage(sourceText) {
    const trimmed = safeString(sourceText).trim();
    if (!trimmed || (!trimmed.startsWith("{") && !trimmed.startsWith("["))) {
      return null;
    }
    const parsed = JSON.parse(trimmed);
    const siteList = Array.isArray(parsed)
      ? parsed
      : Array.isArray(parsed.site)
        ? parsed.site
        : Array.isArray(parsed.sites)
          ? parsed.sites
          : [];
    if (!siteList.length) return null;
    const settingList = Array.isArray(parsed.setting)
      ? parsed.setting
      : parsed.setting && typeof parsed.setting === "object"
        ? [parsed.setting]
        : [];
    const defaultSite = safeString(settingList[0] && settingList[0].defaultSite);
    const activeSites = siteList.filter(function (site) {
      return site && site.isActive !== false;
    });
    const preferred =
      activeSites.find(function (site) {
        return safeString(site.id) === defaultSite || safeString(site.key) === defaultSite;
      }) ||
      activeSites.find(function (site) {
        return Number(site.type) === 7;
      }) ||
      activeSites[0];
    if (!preferred) return null;
    return {
      raw: parsed,
      site: preferred,
    };
  }

  const extCache = {
    key: "",
    text: "",
  };

  function loadRuleSource(site) {
    const rawExt = site && site.ext;
    if (typeof rawExt === "string" && isUrl(rawExt)) {
      if (extCache.key === rawExt && extCache.text) {
        return extCache.text;
      }
      const text = requestText(rawExt, {});
      extCache.key = rawExt;
      extCache.text = text;
      return text;
    }
    extCache.key = "";
    extCache.text = safeString(rawExt);
    return extCache.text;
  }

  function parseRule(source) {
    try {
      const factory = new Function(
        "window",
        source +
          "\n//# sourceURL=zyfun_rule.js" +
          "\nreturn typeof rule !== 'undefined' ? rule : (window.rule || null);",
      );
      const parsedRule = factory(window);
      if (!parsedRule) {
        throw new Error("未找到 rule 对象");
      }
      return parsedRule;
    } catch (error) {
      throw new Error("规则解析失败: " + error.message);
    }
  }

  function parseClasses(rule) {
    const names = safeString(rule.class_name).split("&").filter(Boolean);
    const urls = safeString(rule.class_url).split("&").filter(Boolean);
    const list = [];
    for (let i = 0; i < Math.min(names.length, urls.length); i += 1) {
      list.push({
        name: names[i],
        typeId: urls[i],
      });
    }
    return list;
  }

  function mergeHeaders(baseHeaders, extraHeaders) {
    return Object.assign({}, baseHeaders || {}, extraHeaders || {});
  }

  function requestRule(rule, url, options) {
    const merged = Object.assign({}, options || {});
    merged.headers = mergeHeaders(rule.headers, merged.headers);
    return requestText(ensureAbsolute(rule.host, url), merged);
  }

  function setupHelpers(rule, input, page) {
    window.rule = rule;
    window.input = input;
    window.MY_URL = input;
    window.MY_PAGE = page || 1;
    window.MY_PAGECOUNT = 999;
    window.MY_TOTAL = 9999;
    window.MY_FL = {};
    window.VOD = null;
    window.__drpyResult = null;

    window.setResult = function (value) {
      window.__drpyResult = value;
      return value;
    };
    window.request = function (url, options) {
      return requestRule(rule, url, options || {});
    };
    window.fetch = window.request;
    window.post = function (url, body, options) {
      const merged = Object.assign({}, options || {}, {
        method: "POST",
        body: body || "",
      });
      return requestRule(rule, url, merged);
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
    window.pdfa = function (html, parse) {
      try {
        return JSON.parse(AndroidBridge.pdfa(safeString(html), safeString(parse)));
      } catch (error) {
        return [];
      }
    };
    window.pdfh = function (html, parse, baseUrl) {
      return AndroidBridge.pdfh(safeString(html), safeString(parse), safeString(baseUrl || window.MY_URL || rule.host));
    };
    window.pd = function (html, parse, baseUrl) {
      return AndroidBridge.pd(safeString(html), safeString(parse), safeString(baseUrl || window.MY_URL || rule.host));
    };
    window.pdfl = function (html, parse, listText, listUrl, urlKey) {
      try {
        return JSON.parse(
          AndroidBridge.pdfl(
            safeString(html),
            safeString(parse),
            safeString(listText),
            safeString(listUrl),
            safeString(urlKey || window.MY_URL || rule.host),
          ),
        );
      } catch (error) {
        return [];
      }
    };
    window.getItem = function (key, value) {
      return AndroidBridge.localGet(safeString(rule.key || rule.title || rule.host), safeString(key), safeString(value || ""));
    };
    window.setItem = function (key, value) {
      return AndroidBridge.localSet(safeString(rule.key || rule.title || rule.host), safeString(key), safeString(value || ""));
    };
    window.clearItem = function (key) {
      return AndroidBridge.localDelete(safeString(rule.key || rule.title || rule.host), safeString(key));
    };
    window.batchFetch = function (list) {
      try {
        return JSON.parse(AndroidBridge.batchRequest(JSON.stringify(Array.isArray(list) ? list : [])));
      } catch (error) {
        return [];
      }
    };
    window.log = function () {
      AndroidBridge.log(Array.prototype.slice.call(arguments).join(" "));
    };
  }

  function runJsBlock(block, rule, input, page) {
    setupHelpers(rule, input, page);
    if (typeof block === "function") {
      block();
    } else if (typeof block === "string") {
      const code = block.indexOf("js:") === 0 ? block.substring(3) : block;
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
    if (window.input !== input) {
      return window.input;
    }
    return null;
  }

  function parseSelectorList(html, expr, baseUrl) {
    const parts = safeString(expr).split(";");
    if (parts.length < 5) {
      return [];
    }
    const items = window.pdfa(html, parts[0]);
    return items
      .map(function (itemHtml) {
        return {
          title: safeString(window.pdfh(itemHtml, parts[1], baseUrl)),
          img: safeString(window.pdfh(itemHtml, parts[2], baseUrl)),
          desc: safeString(window.pdfh(itemHtml, parts[3], baseUrl)),
          url: ensureAbsolute(baseUrl, window.pdfh(itemHtml, parts[4], baseUrl)),
        };
      })
      .filter(function (item) {
        return item.title && item.url;
      });
  }

  function replaceTabIndex(value, index) {
    return safeString(value).replace(/#id/g, String(index));
  }

  function splitDescFields(descFields) {
    const fields = safeString(descFields).split(";").filter(Boolean);
    return {
      remarks: fields[0] || "",
      year: fields[1] || "",
      area: fields[2] || "",
      actor: fields[3] || "",
      director: fields[4] || "",
    };
  }

  function parseDetailBySelector(rule, html, detailRule) {
    const descMap = splitDescFields(detailRule.desc);
    const result = {
      title: safeString(window.pdfh(html, detailRule.title, rule.host)),
      img: safeString(window.pdfh(html, detailRule.img, rule.host)),
      remarks: descMap.remarks ? safeString(window.pdfh(html, descMap.remarks, rule.host)) : "",
      year: descMap.year ? safeString(window.pdfh(html, descMap.year, rule.host)) : "",
      area: descMap.area ? safeString(window.pdfh(html, descMap.area, rule.host)) : "",
      director: descMap.director ? safeString(window.pdfh(html, descMap.director, rule.host)) : "",
      actor: descMap.actor ? safeString(window.pdfh(html, descMap.actor, rule.host)) : "",
      content: safeString(window.pdfh(html, detailRule.content, rule.host)),
      playGroups: [],
    };

    const tabItems = detailRule.tabs ? window.pdfa(html, detailRule.tabs) : [];
    if (!tabItems.length) {
      const groups = parseEpisodes(html, detailRule, 0, "榛樿绾胯矾", rule.host);
      if (groups.episodes.length) {
        result.playGroups.push(groups);
      }
      return result;
    }

    tabItems.forEach(function (tabHtml, index) {
      const tabName = safeString(
        window.pdfh(
          tabHtml,
          detailRule.tab_text || detailRule.list_text || "body&&Text",
          rule.host,
        ),
      ) || "绾胯矾" + (index + 1);
      const group = parseEpisodes(html, detailRule, index, tabName, rule.host);
      if (group.episodes.length) {
        result.playGroups.push(group);
      }
    });
    return result;
  }

  function parseEpisodes(html, detailRule, index, tabName, baseUrl) {
    const selector = replaceTabIndex(detailRule.lists, index);
    const itemList = window.pdfa(html, selector);
    const episodes = itemList
      .map(function (itemHtml) {
        const name = safeString(window.pdfh(itemHtml, detailRule.list_text || "body&&Text", baseUrl));
        const url = ensureAbsolute(baseUrl, window.pdfh(itemHtml, detailRule.list_url || "a&&href", baseUrl));
        return {
          name: name || "姝ｇ墖",
          url: url,
        };
      })
      .filter(function (episode) {
        return episode.url;
      });
    return {
      name: tabName,
      episodes: episodes,
    };
  }

  function normalizeJsDetail(detail) {
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
      playGroups: normalizePlayGroups(detail),
    };
  }

  function normalizePlayGroups(detail) {
    const playFrom = safeString(detail.vod_play_from).split("$$$").filter(Boolean);
    const playUrls = safeString(detail.vod_play_url).split("$$$").filter(Boolean);
    const groups = [];
    for (let i = 0; i < Math.min(playFrom.length, playUrls.length); i += 1) {
      const name = playFrom[i] || "绾胯矾" + (i + 1);
      const episodes = playUrls[i]
        .split("#")
        .filter(Boolean)
        .map(function (episode) {
          const parts = episode.split("$");
          return {
            name: safeString(parts[0] || "姝ｇ墖"),
            url: safeString(parts.slice(1).join("$")),
          };
        })
        .filter(function (episode) {
          return episode.url;
        });
      if (episodes.length) {
        groups.push({
          name: name,
          episodes: episodes,
        });
      }
    }
    return groups;
  }

  function normalizePlay(play) {
    if (typeof play === "string") {
      return {
        url: play,
        parse: false,
        jx: 0,
        headers: {},
      };
    }
    play = play || {};
    return {
      url: safeString(play.url || play.input || ""),
      parse: !!play.parse,
      jx: Number(play.jx || 0),
      headers: play.header || play.headers || {},
    };
  }

  function buildCategoryUrl(rule, typeId, page) {
    return ensureAbsolute(
      rule.host,
      safeString(rule.url)
        .replace(/fyclass/g, typeId)
        .replace(/fypage/g, String(page || 1))
        .replace(/fyfilter/g, ""),
    );
  }

  function buildSearchUrl(rule, keyword, page) {
    const template = safeString(rule.searchUrl || rule.search_url || "");
    return ensureAbsolute(
      rule.host,
      template
        .replace(/\*\*/g, encodeURIComponent(keyword || ""))
        .replace(/fypage/g, String(page || 1)),
    );
  }

  function maybeRunPreprocess(rule) {
    const preprocess = rule["棰勫鐞?];
    if (typeof preprocess === "string" && preprocess.indexOf("js:") === 0) {
      runJsBlock(preprocess, rule, rule.host, 1);
    }
  }

  function executeRule(ruleSource, action, input, page, flag) {
    const rule = parseRule(ruleSource);
    setupHelpers(rule, input, page);
    maybeRunPreprocess(rule);
    const data = {};

    if (action === "home") {
      data.meta = {
        title: safeString(rule.title || rule.host),
        host: safeString(rule.host),
        searchable: !!rule.searchable,
        categories: parseClasses(rule),
      };
      if (typeof rule["鎺ㄨ崘"] === "string" && rule["鎺ㄨ崘"].indexOf("js:") !== 0 && rule["鎺ㄨ崘"].indexOf(";") > -1) {
        const html = requestRule(rule, rule.host, {});
        data.list = parseSelectorList(html, rule["鎺ㄨ崘"], rule.host);
      } else {
        const list = runJsBlock(rule["鎺ㄨ崘"], rule, rule.host, 1);
        data.list = Array.isArray(list) ? list : [];
      }
    } else if (action === "category") {
      if (typeof rule["涓€绾?] === "string" && rule["涓€绾?].indexOf("js:") !== 0 && rule["涓€绾?].indexOf(";") > -1) {
        const url = buildCategoryUrl(rule, input, page);
        const html = requestRule(rule, url, {});
        data.list = parseSelectorList(html, rule["涓€绾?], rule.host);
      } else {
        const list = runJsBlock(rule["涓€绾?], rule, buildCategoryUrl(rule, input, page), page);
        data.list = Array.isArray(list) ? list : [];
      }
    } else if (action === "search") {
      if (typeof rule["鎼滅储"] === "string" && rule["鎼滅储"].indexOf("js:") !== 0 && rule["鎼滅储"].indexOf(";") > -1) {
        const url = buildSearchUrl(rule, input, page);
        const html = requestRule(rule, url, {});
        data.list = parseSelectorList(html, rule["鎼滅储"], rule.host);
      } else {
        const list = runJsBlock(rule["鎼滅储"], rule, buildSearchUrl(rule, input, page), page);
        data.list = Array.isArray(list) ? list : [];
      }
    } else if (action === "detail") {
      if (rule["浜岀骇"] && typeof rule["浜岀骇"] === "object") {
        const detailUrl = ensureAbsolute(rule.host, input);
        const html = requestRule(rule, detailUrl, {});
        data.detail = parseDetailBySelector(rule, html, rule["浜岀骇"]);
      } else {
        const detail = runJsBlock(rule["浜岀骇"], rule, ensureAbsolute(rule.host, input), 1);
        data.detail = normalizeJsDetail(detail);
      }
    } else if (action === "lazy") {
      const lazyBlock = rule["lazy"];
      if (!lazyBlock) {
        data.play = {
          url: safeString(input),
          parse: !!rule.play_parse,
          jx: Number(rule.play_parse ? 1 : 0),
          headers: rule.play_headers || rule.headers || {},
        };
      } else {
        const play = runJsBlock(lazyBlock, rule, input, 1);
        data.play = normalizePlay(play);
        if (!data.play.url && typeof window.input === "object") {
          data.play = normalizePlay(window.input);
        }
        if (!data.play.url && typeof window.input === "string") {
          data.play = normalizePlay(window.input);
        }
        if (!data.play.headers || !Object.keys(data.play.headers).length) {
          data.play.headers = rule.play_headers || rule.headers || {};
        }
      }
    } else {
      throw new Error("涓嶆敮鎸佺殑鍔ㄤ綔: " + action);
    }

    return data;
  }

  window.__nativeRuleRuntime = {
    execute(payloadJson) {
      try {
        const payload = JSON.parse(payloadJson);
        const sourcePackage = parseSourcePackage(payload.source);
        const ruleSource = sourcePackage ? loadRuleSource(sourcePackage.site) : payload.source;
        const data = executeRule(
          ruleSource,
          safeString(payload.action),
          safeString(payload.input),
          Number(payload.page || 1),
          safeString(payload.flag),
        );
        return JSON.stringify({
          ok: true,
          data: data,
        });
      } catch (error) {
        return JSON.stringify({
          ok: false,
          error: error && error.message ? error.message : String(error),
        });
      }
    },
  };
})();
