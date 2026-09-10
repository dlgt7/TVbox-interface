package com.github.catvod.spider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XBPQ 爬虫规则「统一字段命名标准」（中英双语，全项目单一事实源）
 *
 * <h3>设计约定</h3>
 * <ol>
 *   <li><b>规范英文键（canonical）</b>：规则内部唯一使用的键，代码只认它。</li>
 *   <li><b>规范中文名</b>：每个英文键对应一个权威中文名，配置里写中文即可。</li>
 *   <li><b>别名（alias）</b>：历史拼写、其它爬虫的同义键、驼峰/下划线变体，全部收敛到规范英文键。</li>
 *   <li><b>大小写不敏感</b>：英文键先按原文匹配，未命中再按小写匹配（如 {@code homeurl} → {@code homeUrl}）。</li>
 *   <li>未登记的键原样透传，不做任何改写（保证向后兼容）。</li>
 * </ol>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   XBPQKey.norm("分类url")        -> "class_url"
 *   XBPQKey.norm("cateUrl")        -> "class_url"
 *   XBPQKey.norm("CLASS_URL")      -> "class_url"
 *   XBPQKey.cn("class_url")        -> "分类url"
 *   XBPQKey.isKnown("分类url")      -> true
 *   XBPQKey.aliasMap()             -> 仅含「非规范写法 -> 规范键」，供规则加载时归一化
 * }</pre>
 *
 * <h3>新增字段流程</h3>
 * 在下方 {@code TABLE} 对应模块里加一行 {@code k(Module.XXX, "规范英文键", "规范中文名", "别名1", "别名2");}，
 * 中英文与别名自动生效，无需改动 {@link XBPQ}。
 *
 * @author CatVodSpider Team
 * @see XBPQ
 */
public final class XBPQKey {

    /** 字段所属模块（用于分组、文档输出与排错定位） */
    public enum Module {
        /** 基础信息 / 请求 / 调试 */
        BASE,
        /** 分类 */
        CATEGORY,
        /** 列表（首页、分类页） */
        LIST,
        /** 搜索 */
        SEARCH,
        /** 详情 */
        DETAIL,
        /** 线路与播放 */
        PLAY,
        /** 嗅探 */
        SNIFF,
        /** 图片代理 / 弹幕 / 直播 */
        PROXY,
        /** CSS（Jsoup）选择器 */
        CSS
    }

    private XBPQKey() {
    }

    /** 原文 → 规范英文键 */
    private static final Map<String, String> NORM = new HashMap<>();
    /** 小写 → 规范英文键（英文键大小写不敏感兜底） */
    private static final Map<String, String> NORM_LC = new HashMap<>();
    /** 规范英文键 → 规范中文名 */
    private static final Map<String, String> CN = new HashMap<>();
    /** 规范英文键 → 所属模块 */
    private static final Map<String, Module> MODULE = new HashMap<>();
    /** 规范英文键的登记顺序（保证文档输出稳定） */
    private static final List<String> ORDER = new ArrayList<>();

    /**
     * 登记一条字段标准。
     *
     * @param m     所属模块
     * @param en    规范英文键（代码内部唯一使用）
     * @param cn    规范中文名
     * @param alias 可选别名（历史拼写 / 其它爬虫同义键 / 中文简称）
     */
    private static void k(Module m, String en, String cn, String... alias) {
        if (en == null || en.isEmpty()) return;
        if (!NORM.containsKey(en)) ORDER.add(en);
        CN.put(en, cn == null ? en : cn);
        MODULE.put(en, m);
        bind(en, en);
        if (cn != null && !cn.isEmpty()) bind(cn, en);
        if (alias != null) {
            for (String a : alias) bind(a, en);
        }
    }

    private static void bind(String from, String en) {
        if (from == null || from.isEmpty()) return;
        String f = from.trim();
        if (f.isEmpty()) return;
        NORM.put(f, en);
        NORM_LC.put(f.toLowerCase(), en);
    }

    static {
        // ==================== 基础信息 / 请求 / 调试 ====================
        k(Module.BASE, "homeUrl", "主页url", "home_url", "主页", "baseUrl", "BASE_URL");
        k(Module.BASE, "header", "请求头", "headers", "头部集合");
        k(Module.BASE, "headerJson", "公共请求头", "公共头");
        k(Module.BASE, "userHeader", "User", "user");
        k(Module.BASE, "encoding", "编码", "charset"); // 标准模板48：响应编码强制矫正（GBK/GB2312）
        k(Module.BASE, "timeout", "超时");
        k(Module.BASE, "retries", "重试", "retry");
        k(Module.BASE, "startpage", "起始页", "startPage");
        k(Module.BASE, "firstpage", "首页", "homePage");
        k(Module.BASE, "User-Agent", "UserAgent", "ua");
        k(Module.BASE, "Referer", "Referer", "referer");
        k(Module.BASE, "allow_internal", "内网放行", "allowInternal");
        k(Module.BASE, "platform", "平台", "平台类型");
        k(Module.BASE, "siteName", "站名");
        k(Module.BASE, "author", "作者");
        k(Module.BASE, "domainSuffix", "后缀", "域名后缀");
        k(Module.BASE, "dynamic_domain", "域名-c", "动态域名"); // 标准模板55：动态域名链，优先级最高
        k(Module.BASE, "home_url_c", "主页url-c", "备用主页"); // 标准模板55：动态域名失败回退
        k(Module.BASE, "openDebug", "打开调试", "debug");
        k(Module.BASE, "prefMenu", "偏好菜单");
        k(Module.BASE, "actionTabs", "功能入口");
        k(Module.BASE, "antiCrawlTimeout", "反爬超时");
        k(Module.BASE, "errorCodes", "错误码");
        k(Module.BASE, "failCodes", "失败码");
        k(Module.BASE, "successCodes", "成功码");
        k(Module.BASE, "btwaf", "宝塔WAF");
        // 写法说明中登记的 WebView 渲染键：注册以便配置归一化与提示，XBPQ 当前版本未实现渲染逻辑
        k(Module.BASE, "web_view", "WebView渲染", "webview");
        k(Module.BASE, "web_view_wait", "WebView等待");
        k(Module.BASE, "web_view_ready", "WebView就绪标记");
        k(Module.BASE, "web_view_ready_timeout", "WebView就绪超时");
        k(Module.BASE, "js_key_url", "滑块Key接口");
        k(Module.BASE, "ocr_api", "打码接口");
        k(Module.BASE, "verify_type", "验证类型");
        // —— 标准模板补齐（模板12/18/23/38/81/92/93）——
        k(Module.BASE, "request_interval", "请求间隔", "requestInterval", "限流间隔");
        k(Module.BASE, "mirror_hosts", "镜像域名", "mirrorHosts", "备用域名");
        k(Module.BASE, "page_end_marker", "翻页终点标记", "pageEndMarker", "分页结束标记");
        k(Module.BASE, "offset_paging", "偏移分页", "offsetPaging");
        k(Module.BASE, "page_size", "每页条数", "pageSize", "pagesize");
        // —— 标准模板补齐（模板30/97）——
        k(Module.BASE, "url_append", "参数追加", "urlAppend", "全局参数", "追加参数");
        // —— 标准模板补齐（模板84）——
        k(Module.BASE, "default_pic", "默认封面", "defaultPic");

        // ==================== 分类 ====================
        k(Module.CATEGORY, "class_url", "分类url", "cateUrl", "list_url");
        k(Module.CATEGORY, "fenlei", "分类", "cate");
        k(Module.CATEGORY, "class_name", "分类名称", "cateName");
        k(Module.CATEGORY, "class_value", "分类值", "cateValue");
        k(Module.CATEGORY, "cat_twice", "分类二次截取", "cateTwice");
        k(Module.CATEGORY, "cat_array", "分类数组", "cateArray");
        k(Module.CATEGORY, "cat_title", "分类标题", "cateTitle");
        k(Module.CATEGORY, "cat_id", "分类ID", "cateId");
        k(Module.CATEGORY, "special_cate", "特殊分类链接");
        k(Module.CATEGORY, "filter", "筛选", "分类筛选");
        k(Module.CATEGORY, "filterdata", "筛选数据");
        k(Module.CATEGORY, "cat_mode", "分类截取模式");
        k(Module.CATEGORY, "catjsonlist", "分类JSON列表");
        k(Module.CATEGORY, "catjsonname", "分类JSON名称");
        k(Module.CATEGORY, "catjsonid", "分类JSONID");
        k(Module.CATEGORY, "catjsonpic", "分类JSON图片");
        k(Module.CATEGORY, "catjsonnote", "分类JSON状态");
        k(Module.CATEGORY, "fclass_name", "筛选子分类名称");
        k(Module.CATEGORY, "fclass_value", "筛选子分类替换词");
        k(Module.CATEGORY, "fcatelog_name", "筛选类型名称");
        k(Module.CATEGORY, "fcatelog_value", "筛选类型替换词");
        k(Module.CATEGORY, "farea_name", "筛选地区名称");
        k(Module.CATEGORY, "farea_value", "筛选地区替换词");
        k(Module.CATEGORY, "fyear_name", "筛选年份名称");
        k(Module.CATEGORY, "fyear_value", "筛选年份替换词");
        k(Module.CATEGORY, "flang_name", "筛选语言名称");
        k(Module.CATEGORY, "flang_value", "筛选语言替换词");
        k(Module.CATEGORY, "fsort_name", "筛选排序名称");
        k(Module.CATEGORY, "fsort_value", "筛选排序替换词");
        k(Module.CATEGORY, "sort_type", "排序", "sortType");
        k(Module.CATEGORY, "sort_value", "排序值", "sortValue");
        k(Module.CATEGORY, "hot_recommend", "热门推荐");
        k(Module.CATEGORY, "list_display", "列表显示");
        // —— 标准模板补齐（模板90）——
        k(Module.CATEGORY, "cate_whitelist", "分类白名单", "cateWhitelist");

        // ==================== 列表 ====================
        k(Module.LIST, "list_array", "数组");
        k(Module.LIST, "list_twice", "二次截取", "列表二次截取");
        k(Module.LIST, "list_name", "标题");
        k(Module.LIST, "list_id", "链接");
        k(Module.LIST, "list_pic", "图片");
        k(Module.LIST, "list_remarks", "副标题", "分类副标题", "cat_subtitle");
        k(Module.LIST, "list_prefix", "链接前缀", "分类片单链接前缀", "cat_prefix");
        k(Module.LIST, "list_suffix", "链接后缀", "分类片单链接后缀", "cat_suffix");
        k(Module.LIST, "filter_word", "过滤词");
        k(Module.LIST, "reverse", "倒序", "倒序播放");
        k(Module.LIST, "list_mode", "列表模式");
        k(Module.LIST, "listjsonlist", "列表JSON路径");
        k(Module.LIST, "listjsonname", "列表JSON名称");
        k(Module.LIST, "listjsonid", "列表JSONID");
        k(Module.LIST, "listjsonpic", "列表JSON图片");
        k(Module.LIST, "listjsonnote", "列表JSON状态");
        // —— 标准模板补齐（模板100）——
        k(Module.LIST, "skip_bad_item", "坏条目跳过", "skipBadItem");
        k(Module.LIST, "list_total", "列表总页数规则", "listTotal", "总页数", "总页数规则", "总数规则");

        // ==================== 搜索 ====================
        k(Module.SEARCH, "search_url", "搜索url");
        k(Module.SEARCH, "search_mode", "搜索模式");
        k(Module.SEARCH, "search_array", "搜索数组");
        k(Module.SEARCH, "search_name", "搜索标题");
        k(Module.SEARCH, "search_pic", "搜索图片");
        k(Module.SEARCH, "search_id", "搜索链接");
        k(Module.SEARCH, "search_remarks", "搜索副标题");
        k(Module.SEARCH, "search_content", "搜索简介");
        k(Module.SEARCH, "search_twice", "搜索二次截取");
        k(Module.SEARCH, "search_header", "搜索请求头");
        k(Module.SEARCH, "search_prefix", "搜索链接前缀");
        k(Module.SEARCH, "search_suffix", "搜索后缀", "搜索链接后缀");
        k(Module.SEARCH, "sea_firstpage", "搜索起始页码", "搜索起始页");
        // POST 搜索：搜索url 末尾 ;post 标记 + POST请求数据（表单 k=v&k=v 或 JSON 字符串）
        k(Module.SEARCH, "post_data", "POST请求数据", "postData", "搜索请求数据", "search_post_data");
        k(Module.SEARCH, "searchjsonlist", "搜索JSON路径");
        k(Module.SEARCH, "searchjsonname", "搜索JSON名称");
        k(Module.SEARCH, "searchjsonid", "搜索JSONID");
        k(Module.SEARCH, "searchjsonpic", "搜索JSON图片");
        k(Module.SEARCH, "searchjsonnote", "搜索JSON状态");
        // —— 标准模板补齐（模板49）——
        k(Module.SEARCH, "search_word_sep", "搜索词分隔符", "searchWordSep", "关键词分隔符");

        // ==================== 详情 ====================
        k(Module.DETAIL, "detail_url", "详情url");
        k(Module.DETAIL, "detail_array", "详情数组");
        k(Module.DETAIL, "detail_twice", "详情二次截取");
        k(Module.DETAIL, "detail_name", "影片名称");
        k(Module.DETAIL, "detail_type", "影片类型", "类型");
        k(Module.DETAIL, "detail_year", "影片年代", "年份");
        k(Module.DETAIL, "detail_area", "影片地区", "地区");
        k(Module.DETAIL, "detail_director", "导演");
        k(Module.DETAIL, "detail_actor", "主演");
        k(Module.DETAIL, "detail_content", "简介", "剧情");
        k(Module.DETAIL, "detail_remarks", "状态");
        k(Module.DETAIL, "detail_separator", "详情分隔符", "detail_label_split");
        k(Module.DETAIL, "detail_merge", "详情合并字段", "detail_content_merge");
        k(Module.DETAIL, "play_image", "播放图片");

        // ==================== 线路与播放 ====================
        k(Module.PLAY, "from_array", "线路数组");
        k(Module.PLAY, "from_title", "线路标题");
        k(Module.PLAY, "line_second_cut", "线路二次截取", "line_twice");
        k(Module.PLAY, "play_array", "播放数组");
        k(Module.PLAY, "url_array", "播放列表");
        k(Module.PLAY, "url_title", "播放标题");
        k(Module.PLAY, "url_url", "播放链接");
        k(Module.PLAY, "play_twice", "播放二次截取");
        k(Module.PLAY, "multi_line_array", "多线数组");
        k(Module.PLAY, "multi_line_url", "多线链接", "线路链接");
        k(Module.PLAY, "multi_line_twice", "多线二次截取");
        k(Module.PLAY, "multi_line_prefix", "多线链接前缀");
        k(Module.PLAY, "multi_line_suffix", "多线链接后缀");
        k(Module.PLAY, "merge_lines", "线路合并");
        k(Module.PLAY, "episode_filter", "剧集过滤");
        k(Module.PLAY, "empty_play_url", "空播放兜底");
        k(Module.PLAY, "empty_play_from", "空播放线路名");
        k(Module.PLAY, "epiurl_prefix", "选集链接加前缀");
        k(Module.PLAY, "epiurl_suffix", "选集链接加后缀");
        k(Module.PLAY, "force_play", "直接播放");
        k(Module.PLAY, "play_prefix", "播放链接前缀");
        k(Module.PLAY, "play_suffix", "播放链接后缀");
        k(Module.PLAY, "play_header", "播放请求头");
        k(Module.PLAY, "jump_url", "跳转播放链接");
        k(Module.PLAY, "Anal_MacPlayer", "分析MacPlayer");
        k(Module.PLAY, "auto_maccms", "自动Maccms");
        // —— 标准模板补齐（模板39/41/46/54/62/63/88）——
        k(Module.PLAY, "episode_sort", "剧集排序", "episodeSort", "选集排序");
        k(Module.PLAY, "episode_dedup", "剧集去重", "episodeDedup", "选集去重");
        k(Module.PLAY, "line_name_map", "线路重命名", "lineNameMap", "线路映射");
        k(Module.PLAY, "play_strip_params", "播放去参数", "playStripParams", "剥离参数");
        // —— 标准模板补齐（模板46）——
        k(Module.PLAY, "episode_quality", "清晰度优先", "episodeQuality", "剧集清晰度", "画质优先");

        // ==================== 嗅探 ====================
        k(Module.SNIFF, "video_format", "嗅探词");
        k(Module.SNIFF, "video_filter", "视频过滤词");
        k(Module.SNIFF, "manualVideoCheck", "免嗅");

        // ==================== 图片代理 / 弹幕 / 直播 ====================
        k(Module.PROXY, "PicNeedProxy", "图片代理");
        k(Module.PROXY, "baseEncodeUrl", "图片代理前缀");
        k(Module.PROXY, "secretKey", "代理密钥");
        k(Module.PROXY, "pic_allow_domains", "图片代理白名单", "picAllowDomains", "代理白名单", "代理域名白名单");
        k(Module.PROXY, "img_attr", "图片属性", "imgAttr", "懒加载属性", "图片真实地址属性");
        k(Module.PROXY, "danmuUrl", "弹幕url");
        k(Module.PROXY, "liveUrl", "直播url");

        // ==================== CSS（Jsoup）选择器 ====================
        k(Module.CSS, "css_selector", "CSS 选择器", "CSS选择器");
        k(Module.CSS, "css_extract", "CSS 提取", "CSS提取");
        k(Module.CSS, "css_attribute", "CSS 属性", "CSS属性");
        k(Module.CSS, "jsoup_parse", "JSOUP 解析", "JSOUP解析");
        k(Module.CSS, "list_css_container", "列表CSS容器");
    }

    /**
     * 把任意写法（中文名 / 英文别名 / 任意大小写）归一化为规范英文键；未登记则原样返回。
     */
    public static String norm(String key) {
        if (key == null || key.isEmpty()) return key;
        String v = NORM.get(key.trim());
        if (v != null) return v;
        v = NORM_LC.get(key.trim().toLowerCase());
        return v != null ? v : key;
    }

    /**
     * 是否为已登记的字段（含规范英文键本身）。
     */
    public static boolean isKnown(String key) {
        if (key == null || key.isEmpty()) return false;
        String t = key.trim();
        return NORM.containsKey(t) || NORM_LC.containsKey(t.toLowerCase());
    }

    /**
     * 规范英文键 → 规范中文名；未登记返回原键。
     */
    public static String cn(String en) {
        if (en == null) return null;
        String v = CN.get(en);
        return v == null ? en : v;
    }

    /**
     * 规范英文键 → 所属模块；未登记返回 null。
     */
    public static Module module(String en) {
        return en == null ? null : MODULE.get(en);
    }

    /**
     * 全部「非规范写法 → 规范英文键」映射（只读）。
     * 供规则加载时做一次性归一化；规范英文键不在其中，避免无谓重写打乱规则键序。
     */
    public static Map<String, String> aliasMap() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, String> e : NORM.entrySet()) {
            if (!e.getKey().equals(e.getValue())) map.put(e.getKey(), e.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * 按登记顺序返回全部规范英文键（只读）。
     */
    public static List<String> keys() {
        return Collections.unmodifiableList(ORDER);
    }

    /**
     * 输出字段标准速查表（调试/文档用），形如：
     * <pre>[分类] class_url | 分类url | cateUrl, list_url</pre>
     */
    public static String dump() {
        StringBuilder sb = new StringBuilder("XBPQ 字段命名标准 (" + ORDER.size() + " 项):\n");
        Module current = null;
        for (String en : ORDER) {
            Module m = MODULE.get(en);
            if (m != current) {
                current = m;
                sb.append('[').append(m == null ? "?" : m.name()).append("]\n");
            }
            sb.append("  ").append(en).append(" | ").append(CN.get(en)).append('\n');
        }
        return sb.toString();
    }
}
