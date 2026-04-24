var rule = {
    title: '吃瓜',
    host: 'https://51cg.fun',
    class_name: '短视频&女优&吃瓜',
    class_url: 'duanshipin&nvyou&chigua',
    searchable: 2,
    url: 'https://51cg.fun/fyclass/page/fypage',
    searchUrl: 'https://51cg.fun/search/**',
    推荐: 'js:\n' +
        'let html = request(input);\n' +
        "let list = html.match(/<div class='tab-content'>(.*?)<footer/s)[1].match(/<div class='col col-12 col-md-6 col-xl-4 mt-3'>[\\s\\S]*?<\\/a>/g);\n" +
        'let rs = [];\n' +
        'list.forEach(it => {\n' +
        "  let url = it.match(/href=\"(.*?)\"/)[1];\n" +
        "  let title = it.match(/<h5>(.*?)<\\/h5>/)[1].replace(/\\s+/g, '');\n" +
        "  let img = it.match(/data-src=\"(.*?)\"/)[1];\n" +
        "  let desc = it.match(/<small>(.*?)<\\/small>/)[1].replace(/\\s+/g, '');\n" +
        '  rs.push({ title, img, desc, url });\n' +
        '});\n' +
        'setResult(rs);',
    一级: 'js:\n' +
        'let html = request(input);\n' +
        "let list = html.match(/<div class='tab-content'>(.*?)<footer/s)[1].match(/<div class='col col-12 col-md-6 col-xl-4 mt-3'>[\\s\\S]*?<\\/a>/g);\n" +
        'let rs = [];\n' +
        'list.forEach(it => {\n' +
        "  let url = it.match(/href=\"(.*?)\"/)[1];\n" +
        "  let title = it.match(/<h5>(.*?)<\\/h5>/)[1].replace(/\\s+/g, '');\n" +
        "  let img = it.match(/data-src=\"(.*?)\"/)[1];\n" +
        "  let desc = it.match(/<small>(.*?)<\\/small>/)[1].replace(/\\s+/g, '');\n" +
        '  rs.push({ title, img, desc, url });\n' +
        '});\n' +
        'setResult(rs);',
    搜索: 'js:\n' +
        'let html = request(input);\n' +
        "let list = html.match(/<div class='tab-content'>(.*?)<footer/s)[1].match(/<div class='col col-12 col-md-6 col-xl-4 mt-3'>[\\s\\S]*?<\\/a>/g);\n" +
        'let rs = [];\n' +
        'list.forEach(it => {\n' +
        "  let url = it.match(/href=\"(.*?)\"/)[1];\n" +
        "  let title = it.match(/<h5>(.*?)<\\/h5>/)[1].replace(/\\s+/g, '');\n" +
        "  let img = it.match(/data-src=\"(.*?)\"/)[1];\n" +
        "  let desc = it.match(/<small>(.*?)<\\/small>/)[1].replace(/\\s+/g, '');\n" +
        '  rs.push({ title, img, desc, url });\n' +
        '});\n' +
        'setResult(rs);',
    二级: 'js:\n' +
        'let html = request(input);\n' +
        "let vod_name = html.match(/<meta property=\"og:title\" content=\"(.*?)\" \\/>/)[1];\n" +
        "let vod_pic = html.match(/<meta property=\"og:image\" content=\"(.*?)\" \\/>/)[1];\n" +
        "let vod_actor = html.match(/<meta name=\"keywords\" content=\"(.*?)\" \\/>/)[1];\n" +
        "let content = html.match(/<meta name=\"description\" content=\"(.*?)\" \\/>/)[1];\n" +
        "let play = html.match(/<iframe class=\"videoIframe\" src=\"(.*?)\" /)[1].replace('http://cg.rgfwzx.com/player/', '');\n" +
        "VOD = {\n" +
        "  vod_name: vod_name,\n" +
        "  vod_pic: vod_pic,\n" +
        "  vod_actor: vod_actor,\n" +
        "  vod_content: content,\n" +
        "  vod_play_from: '道长在线',\n" +
        "  vod_play_url: vod_name + '$' + play,\n" +
        '};',
    lazy: 'js:\n' +
        'let html = request("https://www.m3u8.tv.cdn.8old.cn/m3u8tv/api.php?url=" + input).match(/<body>(.*?)<\\/body>/)[1];\n' +
        'let vod = JSON.parse(html);\n' +
        'input = {\n' +
        '    parse: 0,\n' +
        '    jx: 0,\n' +
        '    url: vod.url,\n' +
        '    header: rule.headers\n' +
        '};'
};
