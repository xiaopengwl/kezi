from pathlib import Path

BASE = Path('/root/work/chigua-android-app/app/src/main/java/com/example/chiguaapp')
PLAYER = (BASE / 'PlayerActivity.java').read_text()
DRPY = (BASE / 'DrpyEngine.java').read_text()
DETAIL = (BASE / 'DetailActivity.java').read_text()
MAIN = (BASE / 'MainActivity.java').read_text()


def test_player_has_sniffer_rule_support():
    assert 'sniffer_match' in PLAYER or 'snifferMatchRules' in PLAYER
    assert 'sniffer_exclude' in PLAYER or 'snifferExcludeRules' in PLAYER


def test_player_has_recursive_iframe_sniffing():
    assert 'loadSniffFrame(' in PLAYER
    assert 'maxSniffDepth' in PLAYER or 'sniffDepth' in PLAYER
    assert 'iframe' in PLAYER.lower()
    assert 'normalizeSniffUrl(' in PLAYER
    assert 'new URL(raw, base).toString()' in PLAYER


def test_player_has_granular_sniffer_rule_pipeline():
    required = [
        'parseRuleValues(source == null ? "" : source.raw, "sniffer_match"',
        'parseRuleValues(source == null ? "" : source.raw, "sniffer_exclude"',
        'parseRuleValues(source == null ? "" : source.raw, "sniffer_follow"',
        'parseRuleValues(source == null ? "" : source.raw, "sniffer_media"',
        'pickRuleScalar(source == null ? "" : source.raw, "sniffer_depth"',
    ]
    missing = [item for item in required if item not in PLAYER]
    assert not missing, f'missing sniffer rule hooks: {missing}'


def test_drpy_runtime_has_extended_compat_layer():
    required = [
        'pdfh', 'pdfa', 'pd(', 'getHtml', 'getHome', 'buildUrl',
        'MOBILE_UA', 'String.prototype.strip', 'jsonParseSafe',
        'requestRaw', 'request(url,opt)', 'fetch_params.headers', 'rule_fetch_params.headers',
        'new URL(String(rel||\'\'), String(base||rule.host||HOST)).toString()',
    ]
    missing = [item for item in required if item not in DRPY]
    assert not missing, f'missing runtime compat hooks: {missing}'


def test_detail_still_passes_episode_arrays_to_player():
    assert 'putStringArrayListExtra("episode_names"' in DETAIL
    assert 'putStringArrayListExtra("episode_inputs"' in DETAIL
    assert 'putExtra("episode_index"' in DETAIL


def test_detail_guards_missing_url_before_running_parser():
    assert 'if(url==null||url.trim().length()==0)' in DETAIL
    assert 'showFallbackDetail("缺少详情地址")' in DETAIL


def test_player_has_netflix_bilibili_mobile_shell():
    assert '奈飞 / 哔哩播放器' in PLAYER
    assert '沉浸式播放' in PLAYER
    assert '▶ 继续播放' in PLAYER
    assert '⏮ 上一集' in PLAYER
    assert '⏭ 下一集' in PLAYER


def test_main_has_streaming_style_bottom_nav():
    assert '想看什么直接搜' in MAIN
    assert '今日主推 / 奈飞式大卡片' in MAIN
    assert '首页' in MAIN and '搜索' in MAIN and '片库' in MAIN and '我的源' in MAIN
