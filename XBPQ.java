package com.github.catvod.spider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.SliderVerifyUtils;
import com.github.catvod.utils.Util;
import com.github.catvod.net.OkHttp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XBPQ extends Spider {

    protected static final int BASE64_FLAG = Base64.DEFAULT | Base64.NO_WRAP;

    private static final List<String> DEFAULT_VIDEO_FORMATS = Arrays.asList(
            ".m3u8", ".mp4", ".mpeg", ".flv", ".mkv"
    );

    private static final int MAX_HTML_LENGTH = 2 * 1024 * 1024;

    private static final int MAX_MATCH_COUNT = 30;

    private static final int MAX_EPISODE_COUNT = 500;

    private static final int MAX_PLAY_LINES = 30;

    private static final int MAX_PAGE_ITEMS = 100;

    private static final int DEFAULT_UNKNOWN_PAGE_COUNT = 50;

    private static final int DEFAULT_HOME_MAX_VIDEOS = 20;

    private static final String[] DEFAULT_TITLE_BOUNDS = {">", "<"};

    private static final String UA_PC = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.54 Safari/537.36";
    private static final String UA_MOBILE = "Mozilla/5.0 (Linux; Android 11; Mi 10 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.152 Mobile Safari/537.36";

    protected String ext = null;

    public JSONObject rule = null;

    private List<String> videoFormatList = new ArrayList<>(DEFAULT_VIDEO_FORMATS);

    private boolean reverseOrder = false;

    private String splitFlag = "";

    private final Map<String, String> headerMap = new ConcurrentHashMap<>();

    private String baseEncodeUrl = "";

    private String secretKey = "";

    private String staticHomeUrl = "";

    private static volatile XBPQ activeInstance = null;

    private final Set<String> proxyAllowedOrigins = ConcurrentHashMap.newKeySet();

    private boolean isDebug = false;

    private int lastResponseCode = 200;

    private boolean requestFailed = false;

    private String failMessage = "";

    private final Map<String, String> variableMap = new ConcurrentHashMap<>();

    private final Random random = new Random();

    private String playImage = "";

    private boolean mergeLines = false;

    private boolean hotRecommend = false;

    private boolean listDisplay = false;

    private static final Pattern P_CSS_EQ = Pattern.compile(":eq\\s*\\(\\s*(\\d+)\\s*\\)");
    
    private static final Pattern P_CSS_INDEX = Pattern.compile("\\[\\s*(\\d+)\\s*\\]$");
    
    private static final int LAST_INDEX = -1;
    
    private static final Pattern P_PROC_MARK = Pattern.compile("\\[(替换|包含|不包含):([^\\]]+)\\]");
    
    private static final Pattern P_ACTION_ATTR = Pattern.compile("action=\"(.+?)\"", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern P_BRACE_VAR = Pattern.compile("\\{(.*?)\\}");
    
    private static final Set<String> KNOWN_BRACE_KEYS = new HashSet<>(Arrays.asList(
            "cateId", "catePg", "cateIdEn", "class", "area", "by", "year", "lang", "letter", "page", "pg",
            
            "offset", "limit"));
    
    private static final Pattern P_PLAYER_OBJ = Pattern.compile("var player_\\w+\\s*=\\s*(\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*\\})");
    
    private static final Pattern P_PLAYER_URL = Pattern.compile("var player_\\w+\\s*=\\s*\\{[\\s\\S]*?\"url\"\\s*:\\s*\"([^\"]+)\"");
    
    private static final Pattern P_ENCRYPT = Pattern.compile("\"encrypt\"\\s*:\\s*(\\d+)");

    private static final Pattern P_VIDEO_DIRECT = Pattern.compile("(?:https?:)?//[^\"'\\s<>]+?\\.(?:m3u8|mp4)(?:\\?[^\"'\\s<>]*)?", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern P_UNICODE_SEQ = Pattern.compile("(\\\\u([0-9A-Fa-f]{4}))");
    
    private static final Pattern P_BTWAF_TOKEN_JSON = Pattern.compile("btwaf[\"'=]\\s*:\\s*[\"']([^\"']+)[\"']");
    
    private static final Pattern P_BTWAF_TOKEN_QUERY = Pattern.compile("[?&]btwaf=([^&\"'\\s>]+)");

    private static final Pattern P_BTWAF_COOKIE = Pattern.compile("(?:^|[;\\s])btwaf=([^;\\s&\"']+)");
    
    private static final Pattern P_META_REFRESH = Pattern.compile("content\\s*=\\s*[\"']\\d+;\\s*url=([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern P_LOCATION_HREF = Pattern.compile("location\\.href\\s*=\\s*[\"']([^\"']+)[\"']");
    
    private static final Pattern P_WINDOW_LOCATION = Pattern.compile("window\\.location\\s*=\\s*[\"']([^\"']+)[\"']");
    
    private static final Pattern P_SELECT_EQ = Pattern.compile("(.+?):eq\\((\\d+)\\)");
    
    private static final Pattern P_TEMPLATE_VAR = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    private static final Pattern P_UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");
    
    private static final Pattern P_CLAN_BRACKET = Pattern.compile("\\[[^\\]\\[]{0,32}\\]");
    
    private static final Pattern P_CLAN_YUAN = Pattern.compile("￥[^￥]{0,32}￥");
    
    private static final Pattern P_HTML_TAG = Pattern.compile("<[^>]+>");
    
    private static final Pattern P_HTML_ENTITY = Pattern.compile("&[a-zA-Z]{1,10};");
    
    private static final Pattern P_RESIDUAL_SYMS = Pattern.compile("[(/>)<]");
    
    private static final Pattern P_WHITESPACE = Pattern.compile("\\s+");
    
    private static final Pattern P_INVISIBLE = Pattern.compile("[\uFEFF\u200B\u200C\u200D\u2060\u00AD]");
    
    private static final Pattern P_HTML_COMMENT = Pattern.compile("<!--.+?-->");
    
    private static final Pattern P_EPISODE_NUM = Pattern.compile("(\\d+)");

    private static final Pattern P_EPISODE_SPAN = Pattern.compile("<span[^>]*>([^<]+)</span>");

    private static final List<String> LAZY_IMG_ATTRS = Collections.unmodifiableList(Arrays.asList(
            "data-original", "data-src", "data-lazy", "data-lazy-src", "data-original-src",
            "data-img", "exposuresrc", "_src", "lazy-src", "data-bg", "src"
    ));

    private static final Set<String> SSRF_BLOCKED_SCHEMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "file", "ftp", "gopher", "dict", "jar", "netdoc", "globalfile", "javascript", "vbscript", "data"
    )));

    private static final Pattern P_INTERNAL_IP = Pattern.compile(
            "^(127\\.|10\\.|192\\.168\\.|169\\.254\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.)"
    );

    private static final List<String> TOTAL_VAR_NAMES = Collections.unmodifiableList(Arrays.asList(
            "mac_total", "mac_page", "pagecount", "total", "totalCount",
            "pages", "count", "limit", "recordCount", "rowCount"
    ));

    private static final List<String> SEARCH_PATH_PROBES = Collections.unmodifiableList(Arrays.asList(
            "/search/", "/index.php?s=", "/vod/search/", "/index.php/ajax/suggest", "/search.php", "/api/search"
    ));

    private static final Pattern P_SETTIMEOUT_LOCATION = Pattern.compile(
            "(?:setTimeout\\s*\\(\\s*function\\s*\\([^)]*\\)\\s*\\{\\s*location(?:\\.href)?\\s*=\\s*|window\\.location\\.href\\s*=\\s*|location\\.replace\\s*\\(\\s*)[\"']([^\"']+)[\"']"
    );

    private static final Pattern P_DETAIL_FIELD_FUZZY = Pattern.compile(
            "(导演|演员|主演|年份|地区|类型|简介|影片导演|主要演员|上映年份|出品地区|影片主演|影片类型|出品时间|更新状态)[^:：>]{0,6}[:：]"
    );

    private static final Pattern P_DATA_URL_ATTR = Pattern.compile(
            "<a[^>]*\\sdata-(?:url|href)\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>([^<]*)</a>"
    );

    private static final Pattern P_JS_PLAYER_CALL = Pattern.compile(
            "(?:javascript:)?player\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)"
    );

    private static final Pattern P_ONCLICK_PLAYER = Pattern.compile(
            "onclick\\s*=\\s*[\"']\\s*(?:javascript:)?player\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)\\s*[\"']"
    );

    private static final Pattern P_TOTAL_VAR_ASSIGN = Pattern.compile(
            "(?:var\\s+|let\\s+|const\\s+|\\$)?(\\w+)\\s*=\\s*(\\d+)"
    );

    private static final Pattern P_TOTAL_JSON = Pattern.compile(
            "[\"'](total|pagecount|totalCount|recordCount|rowCount|count|pages|limit)[\"']\\s*:\\s*(\\d+)"
    );

    private static final Pattern P_META_CHARSET = Pattern.compile(
            "<meta[^>]+charset\\s*=\\s*[\"']?([\\w-]+)", Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> SPECIAL_URL_SCHEMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "magnet", "thunder", "ed2k", "mailto", "javascript"
    )));

    private static final Map<String, String> CHINESE_KEY_MAP = XBPQKey.aliasMap();

    protected final List<String> standardCategoryNames = Arrays.asList(
            "电影", "剧集", "电视剧", "连续剧", "综艺", "动漫"
    );

    protected final List<String> invalidCategoryNames = Arrays.asList(
            "更多", "下载", "首页", "资讯", "留言", "导航", "专题",
            "短视频", "热榜", "排行", "追剧", "更新", "APP",
            "直播", "label", "Netflix"
    );

    protected final List<String> detailFieldNames = Arrays.asList(
            "导演", "主演", "演员", "地区", "类型", "年份", "年代"
    );

    protected final List<String> detailFieldKeys = Arrays.asList(
            "vod_director", "vod_actor", "vod_actor", "vod_area",
            "type_name", "vod_year", "vod_year"
    );

    protected static class HtmlMatchInfo {
        public String group0;           
        public String group1;           
        public String group2;           
        public String diff;             
        public int startPos;            
        public int endPos;              
        public List<Integer> uploads;
        public int matchedUpNodePos = -1;
        public int diffStartIndex;      
        public int diffEndIndex;        

        public void init(Matcher m) {
            this.group0 = m.group(0);
            if (m.groupCount() > 0) this.group1 = m.group(1);
            if (m.groupCount() > 1) this.group2 = m.group(2);
            this.startPos = m.start(0);
            this.endPos = m.end(0);
        }

        public boolean findDiffStr(HtmlMatchInfo rhs, String splitFlag) {
            int len = Math.min(group1.length(), rhs.group1.length());

            for (int i = 0; i < len; ++i) {
                char a = group1.charAt(i);
                char b = rhs.group1.charAt(i);
                if (a == b && splitFlag.indexOf(a) != -1) {
                    diffStartIndex = i + 1;
                    rhs.diffStartIndex = i + 1;
                }
                if (a != b) break;
            }

            diffEndIndex = group1.length();
            rhs.diffEndIndex = rhs.group1.length();
            for (int i = 1; i < len; ++i) {
                char a = group1.charAt(group1.length() - i);
                char b = rhs.group1.charAt(rhs.group1.length() - i);
                if (a == b && splitFlag.indexOf(a) != -1) {
                    diffEndIndex = group1.length() - i;
                    rhs.diffEndIndex = rhs.group1.length() - i;
                }
                if (a != b) break;
            }

            if ((this.diff == null || this.diff.isEmpty()) && diffStartIndex < diffEndIndex) {
                diff = group1.substring(diffStartIndex, diffEndIndex);
            } else {
                if (diffEndIndex < diffStartIndex || !diff.equals(group1.substring(diffStartIndex, diffEndIndex))) {
                    return false;
                }
            }

            if (rhs.diffStartIndex < rhs.diffEndIndex) {
                rhs.diff = rhs.group1.substring(rhs.diffStartIndex, rhs.diffEndIndex);
            }
            return true;
        }

        boolean hasSameUpNode(HtmlMatchInfo rhs) {
            if (rhs.uploads.size() != this.uploads.size()) return false;
            for (int i = 0; i < uploads.size(); ++i) {
                if (uploads.get(i).intValue() != rhs.uploads.get(i).intValue()) continue;
                if (matchedUpNodePos == -1 || uploads.get(i).intValue() == matchedUpNodePos) {
                    matchedUpNodePos = uploads.get(i).intValue();
                    rhs.matchedUpNodePos = uploads.get(i).intValue();
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    public static class HtmlNodeHelper {
        
        private static final List<String> UNPAIRED_TAGS = Arrays.asList(
                "img", "br", "meta", "!--", "input", "hr", "source", "embed",
                "col", "wbr", "base", "area", "param", "track"
        );

        public static boolean isPairedHtmlTag(String str, int startPos) {
            // 按真实标签名判定，禁止用起始片段做子串匹配（旧实现里 10 字符窗口会让 href 命中 hr，
            // 把 <a href=、<li><a href 等成对标签误判为未配对，导致 nodeString 节点截断）。
            String tag = parseHtmlTagName(str, startPos);
            if (tag.isEmpty()) return false;
            return !UNPAIRED_TAGS.contains(tag);
        }

        /** 解析 startPos 处 '<' 开头的标签名（小写）；注释/声明/无法解析时返回空串。 */
        private static String parseHtmlTagName(String str, int startPos) {
            if (str == null || startPos < 0 || startPos >= str.length() || str.charAt(startPos) != '<') return "";
            int i = startPos + 1;
            StringBuilder name = new StringBuilder();
            while (i < str.length()) {
                char c = str.charAt(i);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                    name.append(Character.toLowerCase(c));
                    i++;
                } else {
                    break;
                }
            }
            return name.toString();
        }

        public static boolean isSelfClosedTag(String str, int startPos) {
            for (int i = startPos + 1; i < str.length(); ++i) {
                char c = str.charAt(i);
                if (c == '>') return str.charAt(i - 1) == '/';
                if (c == '<') return false;
            }
            return false;
        }

        public static String nodeString(String str, int pos) {
            if (pos < 0 || pos >= str.length() || str.charAt(pos) != '<') return str;
            int depth = 0;
            for (int i = pos; i < str.length() - 1; ++i) {
                char c = str.charAt(i);
                if (c == '<' && str.charAt(i + 1) == '!' && str.regionMatches(i, "<!--", 0, 4)) {
                    // 注释整体跳过，避免注释内的 '<' '>' 干扰深度统计
                    int end = str.indexOf("-->", i + 4);
                    i = end >= 0 ? end + 2 : str.length() - 2;
                    continue;
                }
                switch (c) {
                    case '/':

                        if (str.charAt(i - 1) == '<') {
                            depth--;
                        }
                        break;
                    case '>':
                        if (depth == 0) return str.substring(pos, i + 1);
                        break;
                    case '<':
                        if (str.charAt(i + 1) != '/' && isPairedHtmlTag(str, i)) {
                            depth++;
                        }
                        break;
                    default:
                        break;
                }
            }
            return str.substring(pos);
        }

        public static List<Integer> findUpNodes(String str, int pos, int lookback) {
            List<Integer> nodes = new ArrayList<>();
            if (pos == -1) return nodes;
            int depth = 0;
            for (int i = pos; i >= 0; --i) {
                switch (str.charAt(i)) {
                    case '/':
                        if (str.charAt(i + 1) == '>') {
                            depth++;
                        } else if (str.charAt(i - 1) == '<') {
                            depth++;
                            --i;
                        }
                        break;
                    case '<':
                        if (depth == 0) {
                            nodes.add(i);
                        } else {
                            
                            if (isPairedHtmlTag(str, i) || isSelfClosedTag(str, i)) {
                                depth--;
                                if (depth < 0) depth = 0;
                            }
                        }
                        break;
                    default:
                        break;
                }
                if (nodes.size() >= lookback) break;
            }
            return nodes;
        }

        public static List<String> getChildNodes(String str) {
            List<String> arr = new ArrayList<>();
            int pos = 0;
            if (pos < 0 || pos >= str.length() || str.charAt(pos) != '<') return arr;
            ++pos;
            while (pos > -1 && pos < str.length()) {
                pos = str.indexOf('<', pos);
                String p = nodeString(str, pos);
                if (p.isEmpty()) break;
                arr.add(p);
                pos += p.length();
            }
            return arr;
        }

        public static String trimHtmlString(String str, String replace) {
            String r = str.replace("\r\n", "").replace("\n", "");
            r = P_HTML_TAG.matcher(r).replaceAll(replace);
            r = P_WHITESPACE.matcher(r).replaceAll(" ");
            return r.replace("&nbsp;", "")
                    .replace("&emsp;", "")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&#39;", "'")
                    .replace("&amp;", "&")
                    .trim();
        }

        public static String trimHtmlString(String str) {
            return trimHtmlString(str, "");
        }
    }

    public static class JsoupExtractor {

        public static final String CSS_PREFIX = "css:";
        public static final String CSS_PREFIX_FULL = "css://";

        private static final char ATTR_MARKER = '@';

        private static final String TEXT_EXTRACT = "@text";
        private static final String OWN_TEXT_EXTRACT = "@ownText";
        private static final String HTML_EXTRACT = "@html";
        private static final String OUTER_HTML_EXTRACT = "@outerHtml";

        public static boolean isCssRule(String rule) {
            return rule != null && (rule.startsWith(CSS_PREFIX_FULL) || rule.startsWith(CSS_PREFIX));
        }

        public static CssRuleInfo parseRule(String rule) {
            if (rule == null || rule.isEmpty()) return null;

            String cleanRule = stripPrefix(rule);

            cleanRule = parseCssShortSyntax(cleanRule);

            ExtractMode mode = ExtractMode.TEXT;
            String attrName = "";
            int index = 0;

            if (cleanRule.contains(TEXT_EXTRACT)) {
                mode = ExtractMode.TEXT;
                cleanRule = cleanRule.replace(TEXT_EXTRACT, "");
            } else if (cleanRule.contains(OWN_TEXT_EXTRACT)) {
                mode = ExtractMode.OWN_TEXT;
                cleanRule = cleanRule.replace(OWN_TEXT_EXTRACT, "");
            } else if (cleanRule.contains(HTML_EXTRACT)) {
                mode = ExtractMode.HTML;
                cleanRule = cleanRule.replace(HTML_EXTRACT, "");
            } else if (cleanRule.contains(OUTER_HTML_EXTRACT)) {
                mode = ExtractMode.OUTER_HTML;
                cleanRule = cleanRule.replace(OUTER_HTML_EXTRACT, "");
            } else if (cleanRule.indexOf(ATTR_MARKER) != -1) {
                
                int atIdx = cleanRule.indexOf(ATTR_MARKER);
                attrName = cleanRule.substring(atIdx + 1).trim();
                cleanRule = cleanRule.substring(0, atIdx);
                mode = ExtractMode.ATTRIBUTE;
            }

            index = parseIndex(cleanRule);
            cleanRule = cleanIndexMarkers(cleanRule);

            cleanRule = cleanRule.trim();

            CssRuleInfo info = new CssRuleInfo();
            info.selector = cleanRule;
            info.mode = mode;
            info.attributeName = attrName;
            info.index = index;
            info.originalRule = rule;
            return info;
        }

        public static String extractSingle(String html, String rule, List<String> result) {
            try {
                Document doc = Jsoup.parse(html);
                CssRuleInfo info = parseRule(rule);
                if (info == null || info.selector.isEmpty()) return "";

                Elements elements = doc.select(info.selector);
                if (elements.isEmpty()) return "";

                Element target = selectByIndex(elements, info.index);
                if (target == null) return "";

                String value = extractValue(target, info.mode, info.attributeName);
                if (!value.isEmpty() && result != null) {
                    result.add(value);
                }
                return value;
            } catch (Exception e) {
                SpiderDebug.log("Jsoup extract error: " + e.getMessage());
            }
            return "";
        }

        public static List<String> extractList(String html, String rule) {
            List<String> results = new ArrayList<>();
            try {
                Document doc = Jsoup.parse(html);
                CssRuleInfo info = parseRule(rule);
                if (info == null || info.selector.isEmpty()) return results;

                Elements elements = doc.select(info.selector);
                for (Element el : elements) {
                    String value = extractValue(el, info.mode, info.attributeName);
                    if (!value.isEmpty()) {
                        results.add(value);
                    }
                }
            } catch (Exception e) {
                SpiderDebug.log("Jsoup extract list error: " + e.getMessage());
            }
            return results;
        }

        public static JSONArray extractItems(String html, String containerRule,
                                             Map<String, String> fieldRules) {
            JSONArray items = new JSONArray();
            try {
                Document doc = Jsoup.parse(html);
                CssRuleInfo containerInfo = parseRule(containerRule);
                if (containerInfo == null || containerInfo.selector.isEmpty()) return items;

                Elements containers = doc.select(containerInfo.selector);
                for (Element container : containers) {
                    JSONObject item = new JSONObject();
                    for (Map.Entry<String, String> entry : fieldRules.entrySet()) {
                        String fieldName = entry.getKey();
                        String fieldRule = entry.getValue();
                        try {
                            CssRuleInfo fieldInfo = parseRule(fieldRule);
                            if (fieldInfo == null) continue;

                            Elements fields = container.select(fieldInfo.selector);
                            Element target = selectByIndex(fields, fieldInfo.index);
                            if (target != null) {
                                String value = extractValue(target, fieldInfo.mode, fieldInfo.attributeName);
                                item.put(fieldName, value);
                            }
                        } catch (Exception e) {
                            SpiderDebug.log("extractItems 字段提取跳过 [" + fieldName + "]: " + e.getMessage());
                        }
                    }

                    if (item.length() > 0) {
                        items.put(item);
                    }
                }
            } catch (Exception e) {
                SpiderDebug.log("Jsoup extract items error: " + e.getMessage());
            }
            return items;
        }

        public static String cutRegion(String html, String rule) {
            try {
                if (!isCssRule(rule)) return html;
                Document doc = Jsoup.parse(html);
                CssRuleInfo info = parseRule(rule);
                if (info == null) return html;

                Elements elements = doc.select(info.selector);
                if (elements.isEmpty()) return html;

                StringBuilder sb = new StringBuilder();
                for (Element el : elements) {
                    sb.append(el.outerHtml());
                }
                return sb.toString();
            } catch (Exception e) {
                SpiderDebug.log("Jsoup cut region error: " + e.getMessage());
            }
            return html;
        }

        public static String smartExtract(String html, String rule) {
            if (isCssRule(rule)) {
                return extractSingle(html, rule, null);
            }
            
            return "";
        }

        private static String stripPrefix(String rule) {
            if (rule.startsWith(CSS_PREFIX_FULL)) {
                return rule.substring(CSS_PREFIX_FULL.length());
            } else if (rule.startsWith(CSS_PREFIX)) {
                return rule.substring(CSS_PREFIX.length());
            }
            return rule;
        }

        private static int parseIndex(String selector) {
            
            Pattern eqPattern = P_CSS_EQ;
            Matcher eqM = eqPattern.matcher(selector);
            if (eqM.find()) {
                return Integer.parseInt(eqM.group(1));
            }
            
            if (selector.contains(":first")) return 0;
            if (selector.contains(":last")) return LAST_INDEX;
            
            Pattern bracketPattern = P_CSS_INDEX;
            Matcher bm = bracketPattern.matcher(selector);
            if (bm.find()) {
                return Integer.parseInt(bm.group(1));
            }
            return 0; 
        }

        private static String cleanIndexMarkers(String selector) {
            return selector.replaceAll(":eq\\s*\\(\\s*\\d+\\s*\\)", "")
                    .replaceAll(":first", "").replaceAll(":last", "")
                    .replaceAll("\\[\\d+\\]$", "")
                    .trim();
        }

        private static Element selectByIndex(Elements elements, int index) {
            if (index >= elements.size()) return null;
            if (index == LAST_INDEX) return elements.last(); 
            return elements.get(index);
        }

        private static String extractValue(Element element, ExtractMode mode, String attrName) {
            switch (mode) {
                case ATTRIBUTE:
                    return element.attr(attrName).trim();
                case TEXT:
                    return element.text().trim();
                case OWN_TEXT:
                    return element.ownText().trim();
                case HTML:
                    return element.html().trim();
                case OUTER_HTML:
                    return element.outerHtml().trim();
                default:
                    return element.text().trim();
            }
        }

        public enum ExtractMode {
            
            TEXT,
            
            OWN_TEXT,
            
            HTML,
            
            OUTER_HTML,
            
            ATTRIBUTE
        }

        public static class CssRuleInfo {
            
            public String selector = "";
            
            public ExtractMode mode = ExtractMode.TEXT;
            
            public String attributeName = "";
            
            public int index = 0;
            
            public String originalRule = "";

            @Override
            public String toString() {
                return String.format("CssRuleInfo{selector='%s', mode=%s, attr='%s', index=%d}",
                        selector, mode.name(), attributeName, index);
            }
        }
    }

    public static class RuleUtils {

        public static int findBlockPos(List<Integer> a, List<Integer> b) {
            if (a == null || b == null) return 0;
            int len = Math.min(a.size(), b.size());
            if (len == 1) return b.get(0);
            for (int i = 0; i < len; ++i) {
                if (a.get(i).intValue() == b.get(i).intValue()) {
                    return i > 0 ? b.get(i - 1) : b.get(0);
                }
            }
            return b.get(len - 1);
        }

        public static String findSubString(String str, int startPos, JSONArray keys, String defaultValue) {
            try {
                if (keys == null) return defaultValue;
                String prefix = keys.getString(0);
                String suffix = keys.getString(1);
                int offsetLeft = keys.length() > 2 ? keys.getInt(2) : 0;
                int offsetRight = keys.length() > 3 ? keys.getInt(3) : 0;

                int start = str.indexOf(prefix, startPos) + prefix.length();
                if (start < prefix.length()) return defaultValue;
                int end = str.indexOf(suffix, start);
                if (end < start) return defaultValue;

                return HtmlNodeHelper.trimHtmlString(str.substring(start + offsetLeft, end + offsetRight));
            } catch (JSONException e) {
                SpiderDebug.log(e);
            }
            return defaultValue;
        }

        public static String findSubString(String str, int startPos, JSONArray keys) {
            return findSubString(str, startPos, keys, "");
        }

        public static int getLookbackCount(JSONArray keys) {
            try {
                if (keys != null && keys.length() > 4) return keys.getInt(4);
            } catch (Exception e) {
                SpiderDebug.log("getLookbackCount 解析失败，回退为0: " + e.getMessage());
            }
            return 0;
        }

        public static JSONArray getLookbackArray(JSONObject obj) {
            try {
                JSONArray preferred = null;
                Iterator<?> iter = obj.keys();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    Object val = obj.get(key);
                    if (val instanceof JSONArray) {
                        int count = getLookbackCount((JSONArray) val);
                        if (count > 0) {
                            
                            if ("vod".equals(key)) return (JSONArray) val;
                            if (preferred == null) preferred = (JSONArray) val;
                        }
                    }
                }
                return preferred;
            } catch (Exception e) {
                SpiderDebug.log(e);
            }
            return null;
        }

        public static int getSubStringCount(String str, String sub) {
            
            if (sub == null || sub.isEmpty()) return 0;
            int pos = 0;
            int count = 0;
            while (pos < str.length()) {
                pos = str.indexOf(sub, pos);
                if (pos == -1) break;
                pos += sub.length();
                ++count;
            }
            return count;
        }

        public static String getRegion(String str, JSONObject obj) {
            try {
                if (obj == null) return str;
                JSONArray region = obj.optJSONArray("region");
                if (region == null) return str;
                String prefix = region.getString(0);
                int start = str.indexOf(prefix);
                if (start == -1) return str;
                int end = str.length();
                if (region.length() > 1) {
                    end = str.indexOf(region.getString(1), start + prefix.length());
                    if (end == -1) end = str.length();
                }
                return str.substring(start, end);
            } catch (JSONException e) {
                SpiderDebug.log(e);
            }
            return str;
        }
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        this.ext = extend;
        activeInstance = this;
    }

    public static final int CODE_OK = 200;
    
    public static final int CODE_FAIL = 500;
    
    public static final String SEP_FROM = "$$$";
    
    public static final String SEP_EPISODE = "#";
    
    public static final String SEP_URL = "$";

    public String BASE_URL = "";

    private String platform = "catvod";

    private void applyStandardTemplateConfig() {
        try {
            BASE_URL = rule.optString("homeUrl", "");
            String p = getRuleVal("platform").trim().toLowerCase();
            platform = p.isEmpty() ? "catvod" : p;
        } catch (Exception e) {
            SpiderDebug.log("applyStandardTemplateConfig: " + e.getMessage());
        }
    }

    public String getBaseUrl() {
        fetchRule();
        return BASE_URL;
    }

    public void setBaseUrl(String url) {
        try {
            fetchRule();
            BASE_URL = url == null ? "" : url.trim();
            if (rule != null && !BASE_URL.isEmpty()) rule.put("homeUrl", BASE_URL);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    private static final List<String> BARE_ARRAY_PLATFORMS = Arrays.asList("hl", "apple");

    protected boolean isBareArrayPlatform() {
        return platform != null && BARE_ARRAY_PLATFORMS.contains(platform.toLowerCase());
    }

    public String getCategory() throws Exception {
        fetchRule();
        JSONArray classes = rule == null ? new JSONArray() : buildClassList(false);
        if (isBareArrayPlatform()) return classes.toString();
        
        JSONObject result = new JSONObject();
        result.put("code", CODE_OK);
        result.put("msg", "ok");
        result.put("list", classes);
        return result.toString();
    }

    public String getVideoList(String tid, String page) throws Exception {
        return categoryContent(tid, page == null ? "1" : page, false, new HashMap<String, String>());
    }

    public String getDetail(String vid) throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add(encodeId(vid));
        return detailContent(ids);
    }

    protected String encodeId(String vid) {
        if (vid == null || vid.isEmpty()) return "";
        try {
            String plain = new String(Base64.decode(vid, BASE64_FLAG), "UTF-8");
            if (plain.trim().startsWith("{")) {
                new JSONObject(plain);
                return vid;
            }
        } catch (Exception ignored) {
            
        }
        try {
            JSONObject o = new JSONObject();
            o.put("vod_id", vid);
            return Base64.encodeToString(o.toString().getBytes(StandardCharsets.UTF_8), BASE64_FLAG);
        } catch (Exception e) {
            SpiderDebug.log("encodeId error: " + e.getMessage());
            return "";
        }
    }

    public String getSearch(String key, String page) throws Exception {
        return searchContent(key, false, page == null ? "1" : page);
    }

    public String playParse(String url) throws Exception {
        return playerContent("", url, new ArrayList<String>());
    }

    protected JSONObject wrapList(JSONArray videos, String body, String pg) throws JSONException {
        int size = videos == null ? 0 : videos.length();
        JSONObject result = new JSONObject();
        result.put("code", CODE_OK);
        result.put("msg", "ok");
        result.put("page", pg == null || pg.trim().isEmpty() ? "1" : pg.trim());
        if (size == 0) {
            int page = 1;
            try {
                page = Integer.parseInt(String.valueOf(pg).trim());
            } catch (Exception ignored) {
                
            }
            result.put("pagecount", Math.max(0, page - 1));
            result.put("total", 0);
        } else {
            
            String lt = getRuleVal("list_total");
            if (!lt.isEmpty()) {
                int ltTotal = -1;
                try {
                    ltTotal = Integer.parseInt(lt);
                } catch (Exception ignored) {
                    if (body != null && !body.isEmpty()) {
                        try {
                            Matcher mLt = Pattern.compile(lt).matcher(body);
                            if (mLt.find()) ltTotal = Integer.parseInt(mLt.group(1));
                        } catch (Exception ignored2) {}
                    }
                }
                if (ltTotal > 0) {
                    result.put("total", ltTotal);
                    int perPage = Math.max(1, size);
                    result.put("pagecount", (ltTotal + perPage - 1) / perPage);
                }
            } else if (body != null && !body.isEmpty()) {
                
                int autoTotal = parseTotalFromHtml(body);
                if (autoTotal > 0) {
                    result.put("total", autoTotal);
                    int perPage = Math.max(1, size);
                    result.put("pagecount", (autoTotal + perPage - 1) / perPage);
                } else {
                    result.put("pagecount", DEFAULT_UNKNOWN_PAGE_COUNT);
                    result.put("total", DEFAULT_UNKNOWN_PAGE_COUNT * Math.max(1, size));
                }
            } else {
                result.put("pagecount", DEFAULT_UNKNOWN_PAGE_COUNT);
                result.put("total", DEFAULT_UNKNOWN_PAGE_COUNT * Math.max(1, size));
            }
        }
        result.put("limit", Math.max(90, size));
        result.put("list", videos == null ? new JSONArray() : videos);
        return result;
    }

    private int parseTotalUniversal(String html) {
        if (html == null || html.isEmpty()) return -1;
        try {
            String lt = getRuleVal("list_total");
            if (!lt.isEmpty()) {
                try {
                    int total = Integer.parseInt(lt);
                    if (total > 0) return total;
                } catch (Exception ignored) {
                    try {
                        Matcher mLt = Pattern.compile(lt).matcher(html);
                        if (mLt.find()) {
                            int total = Integer.parseInt(mLt.group(1));
                            if (total > 0) return total;
                        }
                    } catch (Exception ignored2) {}
                }
            }
            Matcher mTotalKey = P_TOTAL_KEY_TOTAL.matcher(html);
            if (mTotalKey.find()) {
                int total = Integer.parseInt(mTotalKey.group(1));
                if (total > 0) return total;
            }
            Matcher mJson = P_TOTAL_JSON.matcher(html);
            while (mJson.find()) {
                int total = Integer.parseInt(mJson.group(2));
                if (total <= 0) continue;
                return isPageCountVarName(mJson.group(1).toLowerCase()) ? total * 20 : total;
            }
            // 「共N条」是精确总条数；「共N页」是总页数，按每页约20条折算
            Matcher mItems = P_TOTAL_ITEMS_TEXT.matcher(html);
            if (mItems.find()) {
                int total = Integer.parseInt(mItems.group(1));
                if (total > 0) return total;
            }
            Matcher mPages = P_TOTAL_PAGES_TEXT.matcher(html);
            if (mPages.find()) {
                int total = Integer.parseInt(mPages.group(1));
                if (total > 0) return total * 20;
            }
            for (String varName : TOTAL_VAR_NAMES) {
                int total = parseIntFromScript(html, varName);
                if (total > 0) {
                    return isPageCountVarName(varName.toLowerCase()) ? total * 20 : total;
                }
            }
            Matcher mVar = P_TOTAL_VAR_ASSIGN.matcher(html);
            int maxVal = 0;
            while (mVar.find()) {
                String name = mVar.group(1);
                int val = Integer.parseInt(mVar.group(2));
                for (String varName : TOTAL_VAR_NAMES) {
                    if (name.equalsIgnoreCase(varName) || name.toLowerCase().contains(varName.toLowerCase())) {
                        // totalpage 等变量是总页数，必须折算成总条数，否则多页站会算出 pagecount=1
                        if (isPageCountVarName(name.toLowerCase())) val = val * 20;
                        if (val > maxVal) maxVal = val;
                        break;
                    }
                }
            }
            if (maxVal > 0) return maxVal;
            int pageCount = countPageLinks(html);
            if (pageCount > 0) return pageCount * 30;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return -1;
    }

    /** 变量/字段名是否表示「总页数」而非「总条数」 */
    private static boolean isPageCountVarName(String lower) {
        return lower.equals("mac_page") || lower.equals("pages") || lower.equals("pagecount")
                || lower.equals("totalpage") || lower.equals("totalpages") || lower.equals("total_page")
                || lower.equals("pagemax") || lower.equals("maxpage") || lower.equals("page_max");
    }

    private int parseTotalFromHtml(String html) {
        if (html == null || html.isEmpty()) return 0;
        try {
            int universalTotal = parseTotalUniversal(html);
            if (universalTotal > 0) return universalTotal;
            
            int total = parseIntFromScript(html, "mac_total");
            if (total > 0) return total;
            total = parseIntFromScript(html, "mac_page");
            if (total > 0) return total * 20;

            total = parseIntFromAttr(html, "data-total");
            if (total > 0) return total;

            int pageCount = countPageLinks(html);
            if (pageCount > 0) return pageCount * 30;

            int maxPage = extractMaxPageNumber(html);
            if (maxPage > 0) return maxPage * 30;
        } catch (Exception e) {
            SpiderDebug.log("parseTotalFromHtml error: " + e.getMessage());
        }
        return 0;
    }

    private int parseIntFromScript(String html, String varName) {
        try {
            int idx = html.indexOf(varName + "=");
            if (idx < 0) return 0;
            int start = html.indexOf('=', idx) + 1;
            int end = start;
            while (end < html.length() && Character.isDigit(html.charAt(end))) end++;
            if (end > start) {
                return Integer.parseInt(html.substring(start, end));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private int parseIntFromAttr(String html, String attrName) {
        try {
            int idx = html.indexOf(attrName + "=\"");
            if (idx < 0) return 0;
            int start = html.indexOf('"', idx + attrName.length() + 2) + 1;
            int end = start;
            while (end < html.length() && Character.isDigit(html.charAt(end))) end++;
            if (end > start) {
                return Integer.parseInt(html.substring(start, end));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static final Pattern P_PAGE_LI = Pattern.compile("<li[^>]*page[^>]*>\\s*(\\d+)\\s*</li>", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_PAGE_QS = Pattern.compile("[?&]page=(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_TOTAL_KEY_TOTAL = Pattern.compile("[\"']total[\"']\\s*:\\s*(\\d+)");
    private static final Pattern P_TOTAL_ITEMS_TEXT = Pattern.compile("共\\s*(\\d+)\\s*条");
    private static final Pattern P_TOTAL_PAGES_TEXT = Pattern.compile("共\\s*(\\d+)\\s*页");

    private int countPageLinks(String html) {
        int count = 0;
        try {
            java.util.regex.Matcher m = P_PAGE_LI.matcher(html);
            while (m.find()) count++;
            m = P_PAGE_QS.matcher(html);
            java.util.Set<String> pages = new java.util.HashSet<>();
            while (m.find()) pages.add(m.group(1));
            count = Math.max(count, pages.size());
        } catch (Exception ignored) {}
        return count;
    }

    private int extractMaxPageNumber(String html) {
        int maxPage = 0;
        try {
            java.util.regex.Matcher m = P_PAGE_QS.matcher(html);
            while (m.find()) {
                int page = Integer.parseInt(m.group(1));
                if (page > maxPage) maxPage = page;
            }
            m = P_PAGE_LI.matcher(html);
            while (m.find()) {
                int page = Integer.parseInt(m.group(1));
                if (page > maxPage) maxPage = page;
            }
        } catch (Exception ignored) {}
        return maxPage;
    }

    public String httpGet(String url) {
        return fetchUrl(url, null);
    }

    public String httpGet(String url, JSONObject headers) {
        return fetchUrl(url, headers);
    }

    public String httpGetSafe(String url) {
        try {
            return fetchUrl(url, null);
        } catch (Exception e) {
            SpiderDebug.log(safeLog("httpGetSafe error: " + e.getMessage()));
            return "";
        }
    }

    public String httpGetRaw(String url) {
        try {
            if (isInternalUrl(url)) {
                SpiderDebug.log(safeLog("httpGetRaw SSRF blocked: " + url));
                return "";
            }
            return OkHttp.string(url, getHeaders(url));
        } catch (Exception e) {
            SpiderDebug.log(safeLog("httpGetRaw error: " + e.getMessage()));
            return "";
        }
    }

    public String httpPost(String url, Map<String, String> params) {
        return fetchPostForm(url, params, null);
    }

    public String httpPostJson(String url, String json) {
        try {
            if (isInternalUrl(url)) {
                SpiderDebug.log(safeLog("httpPostJson SSRF blocked: " + url));
                return "";
            }
            return OkHttp.post(url, json, getHeaders(url));
        } catch (Exception e) {
            SpiderDebug.log(safeLog("httpPostJson error: " + e.getMessage()));
            return "";
        }
    }

    protected JSONArray extractVideosByJson(String json, String path,
                                            String idKey, String nameKey,
                                            String picKey, String noteKey) {
        JSONArray videos = new JSONArray();
        if (json == null || json.isEmpty() || path == null || path.isEmpty()) return videos;
        try {
            JsonElement el = firstArray(Json.pathFindBy(Json.parse(json), path));
            if (el == null) return videos;
            Set<String> seen = new HashSet<>();
            for (JsonElement item : el.getAsJsonArray()) {
                if (!item.isJsonObject()) continue;
                if (videos.length() >= MAX_PAGE_ITEMS) {
                    SpiderDebug.log("JSON 列表条目已达单页上限 " + MAX_PAGE_ITEMS + "，截断");
                    break;
                }
                JsonObject o = item.getAsJsonObject();
                String id = jsonPick(o, idKey);
                String name = jsonPick(o, nameKey);
                if (id.isEmpty() && name.isEmpty()) continue;
                if (!id.isEmpty()) {
                    if (seen.contains(id)) continue;
                    seen.add(id);
                }
                JSONObject v = new JSONObject();
                v.put("vod_id", applyIdAffix(id));
                v.put("vod_name", name);
                String pic = addHttpPrefix(jsonPick(o, picKey));
                
                if (pic.isEmpty()) pic = addHttpPrefix(getRuleVal("default_pic").trim());
                v.put("vod_pic", pic);
                v.put("vod_remarks", jsonPick(o, noteKey));
                v.put("vod_id", encodeVodId(v));
                videos.put(v);
            }
        } catch (Exception e) {
            SpiderDebug.log("JSON 模式解析失败: " + e.getMessage());
        }
        return videos;
    }

    protected JSONArray extractCategoriesByJson(String json, String path,
                                                String idKey, String nameKey) {
        return extractCategoriesByJson(json, path, idKey, nameKey, "", "");
    }

    protected JSONArray extractCategoriesByJson(String json, String path,
                                                String idKey, String nameKey, String picKey, String noteKey) {
        JSONArray classes = new JSONArray();
        if (json == null || json.isEmpty() || path == null || path.isEmpty()) return classes;
        try {
            JsonElement el = firstArray(Json.pathFindBy(Json.parse(json), path));
            if (el == null) return classes;
            for (JsonElement item : el.getAsJsonArray()) {
                if (!item.isJsonObject()) continue;
                JsonObject o = item.getAsJsonObject();
                String id = jsonPick(o, idKey);
                String name = jsonPick(o, nameKey);
                if (id.isEmpty() || name.isEmpty()) continue;
                JSONObject c = new JSONObject();
                c.put("type_id", id);
                c.put("type_name", name);
                String pic = picKey == null || picKey.isEmpty() ? "" : jsonPick(o, picKey);
                String note = noteKey == null || noteKey.isEmpty() ? "" : jsonPick(o, noteKey);
                if (!pic.isEmpty()) c.put("type_pic", pic);
                if (!note.isEmpty()) c.put("type_note", note);
                classes.put(c);
            }
        } catch (Exception e) {
            SpiderDebug.log("分类 JSON 模式解析失败: " + e.getMessage());
        }
        return classes;
    }

    private static JsonElement firstArray(JsonElement el) {
        if (el == null) return null;
        if (el.isJsonArray()) return el;
        if (!el.isJsonObject()) return null;
        for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
            if (e.getValue().isJsonArray()) return e.getValue();
        }
        return null;
    }

    private static String jsonPick(JsonObject obj, String key) {
        if (obj == null || key == null || key.isEmpty()) return "";
        try {
            if (key.indexOf('.') >= 0 || key.indexOf('[') >= 0) {
                List<String> got = Json.pathGet(obj.toString(), key, "");
                return got.isEmpty() ? "" : got.get(0);
            }
            return Json.getString(obj, key);
        } catch (Exception e) {
            return "";
        }
    }

    private String applyIdAffix(String id) {
        if (id == null || id.isEmpty()) return "";
        return getRuleVal("list_prefix") + id + getRuleVal("list_suffix");
    }

    private boolean tryBuildFromJson(JSONArray classes) throws JSONException {
        String jsonPath = getRuleVal("catjsonlist");
        if (jsonPath.isEmpty()) return false;
        String url = rule.optString("class_url", "");
        
        if (url.contains("{cateId}")) url = "";
        if (url.isEmpty()) url = rule.optString("homeUrl", "");
        if (url.isEmpty()) return false;
        String json = httpGetRaw(addHttpPrefix(url));
        if (json.isEmpty()) return false;
        JSONArray parsed = extractCategoriesByJson(json, jsonPath,
                getRuleVal("catjsonid"), getRuleVal("catjsonname"),
                getRuleVal("catjsonpic"), getRuleVal("catjsonnote"));
        for (int i = 0; i < parsed.length(); i++) classes.put(parsed.get(i));
        return classes.length() > 0;
    }

    protected JSONObject convertChineseKeys(JSONObject json) {
        try {
            
            Set<String> originalKeys = new HashSet<>();
            Iterator<String> originIt = json.keys();
            while (originIt.hasNext()) originalKeys.add(originIt.next());

            List<String> toRename = new ArrayList<>();
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (CHINESE_KEY_MAP.containsKey(key)) {
                    toRename.add(key);
                }
            }
            
            for (String key : toRename) {
                String enKey = CHINESE_KEY_MAP.get(key);
                Object val = json.get(key);
                json.remove(key);
                
                if (!json.has(enKey)
                        || (isEmptyRuleVal(json.opt(enKey)) && !isEmptyRuleVal(val))
                        || (originalKeys.contains(enKey) && !isEmptyRuleVal(val))) {
                    json.put(enKey, val);
                }
            }
            
            List<String> unknownKeys = new ArrayList<>();
            Iterator<String> allIt = json.keys();
            while (allIt.hasNext()) {
                String key = allIt.next();
                if (!CHINESE_KEY_MAP.containsKey(key) && containsCjk(key)) unknownKeys.add(key);
            }
            if (!unknownKeys.isEmpty()) {
                SpiderDebug.log("XBPQ 未识别的中文键(已按原样保留): " + TextUtils.join(", ", unknownKeys));
            }
            
            List<String> allKeys = new ArrayList<>();
            keys = json.keys();
            while (keys.hasNext()) allKeys.add(keys.next());
            for (String key : allKeys) {
                Object val = json.get(key);
                if (val instanceof JSONObject) {
                    json.put(key, convertChineseKeys((JSONObject) val));
                } else if (val instanceof JSONArray) {
                    JSONArray arr = (JSONArray) val;
                    for (int i = 0; i < arr.length(); i++) {
                        if (arr.get(i) instanceof JSONObject) {
                            arr.put(i, convertChineseKeys((JSONObject) arr.get(i)));
                        }
                    }
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return json;
    }

    private static boolean isEmptyRuleVal(Object val) {
        if (!(val instanceof String)) return false;
        String s = (String) val;
        return s.isEmpty() || "空".equals(s) || "&&".equals(s);
    }

    private static boolean containsCjk(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) return true;
        }
        return false;
    }

    protected String getRuleVal(String key, String defaultValue) {
        if (rule == null) return defaultValue;
        String value = rule.optString(key, "");
        if (value.isEmpty() || "空".equals(value) || "&&".equals(value)) {
            return defaultValue;
        }
        return value;
    }

    protected String getRuleVal(String key) {
        return getRuleVal(key, "");
    }

    private static JSONObject normalizeRuleKeys(JSONObject obj) {
        if (obj == null) return obj;
        try {
            JSONObject out = new JSONObject();
            java.util.Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object v = obj.opt(k);
                if (v instanceof JSONObject) {
                    v = normalizeRuleKeys((JSONObject) v);
                } else if (v instanceof JSONArray) {
                    v = normalizeRuleArrayKeys((JSONArray) v);
                }
                String target = XBPQKey.norm(k);
                if (!out.has(target)) {
                    out.put(target, v);
                } else if (isEmptyRuleVal(out.opt(target)) && !isEmptyRuleVal(v)) {
                    out.put(target, v);
                }
            }
            return out;
        } catch (Exception e) {
            SpiderDebug.log(e);
            return obj;
        }
    }

    private static JSONArray normalizeRuleArrayKeys(JSONArray arr) {
        JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            Object v = arr.opt(i);
            if (v instanceof JSONObject) {
                out.put(normalizeRuleKeys((JSONObject) v));
            } else {
                out.put(v);
            }
        }
        return out;
    }

    protected void fetchRule() {
        if (rule == null) {
            if (ext != null) {
                try {
                    JSONObject rawRule;
                    if (ext.startsWith("http")) {
                        
                        if (ext.contains("{cateId}") || ext.contains("{catePg}")) {
                            rule = new JSONObject();
                            rule.put("homeUrl", ext);
                        } else {
                            String json = OkHttp.string(ext, null);
                            rawRule = new JSONObject(json);
                            rule = normalizeRuleKeys(convertChineseKeys(rawRule));
                        }
                    } else {
                        
                        rawRule = new JSONObject(ext);
                        rule = normalizeRuleKeys(convertChineseKeys(rawRule));
                    }

                    initializeRuleConfig();
                    initEnhancedConfig();
                    applyPrefMenu();
                    applyStandardTemplateConfig();

                    SpiderDebug.log(sanitizeRuleLog(rule));
                } catch (Exception e) {
                    SpiderDebug.log(e);
                }
            }
        }
    }

    private static final Set<String> SENSITIVE_RULE_KEYS = new HashSet<>(Arrays.asList(
            "cookie", "secretkey", "password", "passwd", "pwd",
            "token", "accesstoken", "authorization", "auth",
            "header", "headerjson", "userheader", "key", "sign", "signkey", "secret"));

    private String sanitizeRuleLog(JSONObject ruleObj) {
        try {
            if (!isDebug) {
                JSONArray keys = ruleObj.names();
                StringBuilder sb = new StringBuilder("已加载规则 keys=[");
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(keys.getString(i));
                    }
                }
                sb.append("]");
                return sb.toString();
            }
            JSONObject safe = new JSONObject();
            Iterator<String> it = ruleObj.keys();
            while (it.hasNext()) {
                String k = it.next();
                if (SENSITIVE_RULE_KEYS.contains(k.toLowerCase())) {
                    safe.put(k, "***掩码***");
                } else {
                    safe.put(k, ruleObj.get(k));
                }
            }
            return "默认rule: " + safe.toString();
        } catch (Exception e) {
            return "默认rule: <日志生成失败>";
        }
    }

    private void initializeRuleConfig() throws JSONException, MalformedURLException {
        
        if (!rule.has("class_url") && rule.has("list") && rule.getJSONObject("list").has("url")) {
            rule.put("class_url", rule.getJSONObject("list").getString("url"));
        }

        processVideoFormatConfig();

        if (!rule.has("list")) {
            rule.put("list", new JSONObject());
        }
        JSONObject list = rule.getJSONObject("list");

        initializeHomeUrl(list);

        initializeSplitFlag(list);

        if (!rule.has("detail")) {
            rule.put("detail", new JSONObject());
        }

        if (!rule.has("playlist")) {
            rule.put("playlist", new JSONObject());
        }

        initializeSearchConfig();

        initializePlayConfig();

        injectFlatFieldsToList(list);

        applyAllStringCutRules(list);

        processPlaylistFlatFields();

        processDetailFlatFields();

        processLineConfigs();

        reverseOrder = "1".equals(getRuleVal("reverse"));

        guessVodIdIfNeeded(list);

        initializeCssRules(list);
    }

    private void processVideoFormatConfig() {
        if (rule.has("video_format")) {
            String vf = rule.optString("video_format", "");
            if (!vf.isEmpty()) {
                videoFormatList.clear();
                for (String f : vf.split("#")) {
                    if (!f.trim().isEmpty()) videoFormatList.add(f.trim());
                }
            }
        }
    }

    private void initializeHomeUrl(JSONObject list) throws JSONException, MalformedURLException {
        
        if (rule.optString("homeUrl", "").isEmpty()) {
            String derived = deriveHomeUrl(rule.optString("class_url", ""));
            if (derived.isEmpty() && list != null) derived = deriveHomeUrl(list.optString("url", ""));
            if (derived.isEmpty() && list != null) derived = deriveHomeUrl(getRuleVal("list_url"));
            if (!derived.isEmpty()) {
                rule.put("homeUrl", derived);
                SpiderDebug.log("未配置 主页url，已由分类url推导: " + derived);
            }
        }
        if (rule.optString("homeUrl", "").isEmpty()) {
            
            SpiderDebug.log("警告: 未配置 主页url 且无法推导，相对链接将无法补全");
            return;
        }
        String homeUrl = rule.getString("homeUrl");
        if (homeUrl.contains("{cateId}")) {
            URL url = new URL(homeUrl);
            String path = url.getPath();
            rule.put("homeUrl", homeUrl.substring(0, homeUrl.indexOf(path)));
            if (!list.has("url")) {
                list.put("url", homeUrl);
            }
        }
        
        if (!list.has("url")) {
            String classUrl = rule.optString("class_url", "");
            if (!classUrl.isEmpty()) {
                list.put("url", classUrl);
            }
        }
    }

    private static String deriveHomeUrl(String rawUrl) {
        if (rawUrl == null) return "";
        String u = rawUrl.trim();
        if (u.isEmpty()) return "";
        int sep = u.indexOf(";;");
        if (sep >= 0) u = u.substring(0, sep);
        
        u = u.replace("{cateId}", "1")
                .replace("{catePg}", "1")
                .replace("{catepg}", "1")
                .replace("{pg}", "1")
                .replace("{wd}", "x");
        
        u = u.replaceAll("\\{[^}]*\\}", "").trim();
        if (!u.contains("://")) return "";
        try {
            URL parsed = new URL(u);
            String host = parsed.getHost();
            if (host == null || host.isEmpty()) return "";
            return parsed.getProtocol() + "://" + host + (parsed.getPort() > 0 ? ":" + parsed.getPort() : "");
        } catch (MalformedURLException e) {
            return "";
        }
    }

    private void initializeSplitFlag(JSONObject list) {
        String listUrl = list.optString("url", "");
        if (listUrl.contains("/")) splitFlag += '/';
        if (listUrl.contains(".")) splitFlag += '.';
        if (listUrl.contains("-")) splitFlag += '-';
    }

    private void initializeSearchConfig() throws JSONException {
        boolean hasFlatSearch = !getRuleVal("search_url").isEmpty()
                || !getRuleVal("search_array").isEmpty()
                || !getRuleVal("search_name").isEmpty()
                || !getRuleVal("search_pic").isEmpty()
                || !getRuleVal("search_id").isEmpty();

        Object searchObjRaw = rule.opt("search");
        boolean hasSearchStrFormat = searchObjRaw instanceof String && !((String) searchObjRaw).isEmpty();

        if (!rule.has("search") && !hasFlatSearch && !hasSearchStrFormat) {
            generateDefaultSearchConfig();
        }

        if (hasSearchStrFormat) {
            convertSearchStringToJson((String) searchObjRaw);
        }

        if (hasFlatSearch) {
            applyFlatSearchFields();
        }
    }

    private void generateDefaultSearchConfig() {
        try {
            String url = addHttpPrefix("index.php/ajax/suggest?mid=1&wd=" + URLEncoder.encode("test", "UTF-8"));
            JSONObject result = new JSONObject(OkHttp.string(url, getHeaders(url)));
            JSONObject search = new JSONObject();
            search.put("vod_id", "id");
            search.put("vod_name", "name");
            search.put("vod_pic", "pic");
            search.put("url", addHttpPrefix("index.php/ajax/suggest?mid=1&wd={wd}"));
            rule.put("search", search);
        } catch (Exception e) {
            SpiderDebug.log("默认搜索配置生成失败（suggest接口不可用）: " + e.getMessage());
        }
    }

    private void convertSearchStringToJson(String searchUrlStr) throws JSONException {
        JSONObject searchJson = new JSONObject();
        searchJson.put("url", addHttpPrefix(searchUrlStr));
        rule.put("search", searchJson);
    }

    private void applyFlatSearchFields() throws JSONException {
        if (!rule.has("search")) {
            rule.put("search", new JSONObject());
        }
        JSONObject searchObj = rule.getJSONObject("search");
        String searchUrlFlat = getRuleVal("search_url");
        if (!searchUrlFlat.isEmpty()) {
            searchObj.put("url", searchUrlFlat);
        }

        String[][] flatSearchFields = {
                {"search_name", "vod_name"},
                {"search_pic", "vod_pic"},
                {"search_id", "vod_id"},
                {"search_remarks", "vod_remarks"}
        };
        for (String[] pair : flatSearchFields) {
            String value = getRuleVal(pair[0]);
            if (!value.isEmpty()) {
                JSONArray pairArr = stringCutPair(value);
                if (pairArr != null) {
                    searchObj.put(pair[1], pairArr);
                } else {
                    // 无 && 的取值为 JSON 字段名（MacCMS API 惯用写法：搜索链接:vod_id），供 JSON 搜索模式使用
                    searchObj.put(pair[1], value.trim());
                }
            }
        }
    }

    private void initializePlayConfig() throws JSONException {
        if (!rule.has("play")) {
            JSONObject play = new JSONObject();
            JSONArray region = new JSONArray();
            region.put("var player_aaaa=");
            
            region.put("</script>");

            JSONArray vodUrl = new JSONArray();
            vodUrl.put("\"url\":\"");
            
            vodUrl.put("\"");

            play.put("region", region);
            play.put("vod_url", vodUrl);
            rule.put("play", play);
        }

        processPlayKeywords();
    }

    private void processPlayKeywords() throws JSONException {
        if (rule.has("play")) {
            JSONObject play = rule.getJSONObject("play");
            JSONArray keywords = play.optJSONArray("keywords");
            if (keywords != null) {
                videoFormatList.clear();
                for (int i = 0; i < keywords.length(); ++i) {
                    videoFormatList.add(keywords.getString(i));
                }
            }
        }
    }

    private void injectFlatFieldsToList(JSONObject list) throws JSONException {
        String[][] flatListFields = {
                {"list_name", "vod_name"},
                {"list_pic", "vod_pic"},
                {"list_id", "vod_id"},
                {"list_remarks", "vod_remarks"}
        };
        for (String[] pair : flatListFields) {
            String value = getRuleVal(pair[0]);
            if (!value.isEmpty() && !list.has(pair[1])) {
                JSONArray pairArr = stringCutPair(value);
                if (pairArr != null) list.put(pair[1], pairArr);
            }
        }

        JSONObject search = rule.optJSONObject("search");
        if (search != null) {
            for (String[] pair : flatListFields) {
                String value = getRuleVal(pair[0].replace("list_", "search_"));
                if (!value.isEmpty() && !search.has(pair[1])) {
                    JSONArray pairArr = stringCutPair(value);
                    if (pairArr != null) {
                        search.put(pair[1], pairArr);
                    } else if (!value.contains("&&")) {
                        // 同 applyFlatSearchFields：无 && 取值为 JSON 字段名
                        search.put(pair[1], value.trim());
                    }
                }
            }
        }
    }

    private void applyAllStringCutRules(JSONObject list) throws JSONException {
        
        if (list != null && !list.has("vod")) {
            String listArray = getRuleVal("list_array");
            if (!listArray.isEmpty()) {
                JSONArray lookback = stringCutToLookback(applyOrSelector(listArray));
                if (lookback != null) list.put("vod", lookback);
            }
        }
        applyStringCutRules(rule.optJSONObject("search"), "search_array");
        applyStringCutRules(rule.optJSONObject("playlist"), "play_array");
        applyStringCutRules(rule.optJSONObject("playlist"), "from_array");
        applyStringCutRules(rule.optJSONObject("detail"), "detail_array");
    }

    private boolean insideNoParseBlock(String content, int pos) {
        int styleStart = content.lastIndexOf("<style", pos);
        if (styleStart >= 0) {
            int styleEnd = content.indexOf("</style", styleStart);
            if (styleEnd == -1 || styleEnd > pos) return true;
        }
        int scriptStart = content.lastIndexOf("<script", pos);
        if (scriptStart >= 0) {
            int scriptEnd = content.indexOf("</script", scriptStart);
            if (scriptEnd == -1 || scriptEnd > pos) return true;
        }
        return false;
    }

    protected JSONArray stringCutPair(String rule) {        if (rule == null || rule.isEmpty()) return null;
        String cutRule = applyPostProcessors(applyOrSelector(rule));
        if (!cutRule.contains("&&")) return null;
        String[] parts = cutRule.split("&&", 2);
        JSONArray arr = new JSONArray();
        arr.put(parts[0].trim());
        arr.put(parts.length > 1 ? parts[1].trim() : "");
        return arr;
    }

    private void processPlaylistFlatFields() throws JSONException {
        JSONObject playlist = rule.getJSONObject("playlist");

        String urlUrl = getRuleVal("url_url");
        if (!urlUrl.isEmpty() && !playlist.has("vod_play_url")) {
            JSONArray pairArr = stringCutPair(urlUrl);
            if (pairArr != null) playlist.put("vod_play_url", pairArr);
        }
        if (!playlist.has("vod_play_url")) {
            String urlArray = getRuleVal("url_array");
            if (!urlArray.isEmpty()) {
                JSONArray pairArr = stringCutPair(urlArray);
                if (pairArr != null) playlist.put("vod_play_url", pairArr);
            }
        }

        String urlTitle = getRuleVal("url_title");
        if (!urlTitle.isEmpty() && !playlist.has("vod_play_url_title")) {
            JSONArray pairArr = stringCutPair(urlTitle);
            if (pairArr != null) playlist.put("vod_play_url_title", pairArr);
        }
    }

    private void processDetailFlatFields() throws JSONException {
        JSONObject detail = rule.optJSONObject("detail");
        if (detail == null) return;
        String[][] flatDetailFields = {
                {"detail_name", "vod_name"},
                {"detail_type", "type_name"},
                {"detail_year", "vod_year"},
                {"detail_area", "vod_area"},
                {"detail_remarks", "vod_remarks"},
                {"detail_actor", "vod_actor"},
                {"detail_director", "vod_director"},
                {"detail_content", "vod_content"}
        };
        for (String[] pair : flatDetailFields) {
            String value = getRuleVal(pair[0]);
            
            if (value.isEmpty() || detail.has(pair[1])) continue;
            if (!value.contains("&&")) {
                SpiderDebug.log("XBPQ 详情字段 " + pair[0] + " 未包含 && 截取语法，已忽略");
                continue;
            }
            JSONArray pairArr = stringCutPair(value);
            if (pairArr != null) detail.put(pair[1], pairArr);
        }
    }

    private void processLineConfigs() {
        String lineSecondCut = getRuleVal("line_second_cut");
        String multiLineArray = getRuleVal("multi_line_array");
        String multiLineUrl = getRuleVal("multi_line_url");
        String multiLineTwice = getRuleVal("multi_line_twice");
        String multiLinePrefix = getRuleVal("multi_line_prefix");
        String multiLineSuffix = getRuleVal("multi_line_suffix");

        boolean hasLineConfig = !lineSecondCut.isEmpty()
                || (!multiLineArray.isEmpty() && !multiLineUrl.isEmpty())
                || !multiLineTwice.isEmpty();

        if (hasLineConfig) {
            SpiderDebug.log(String.format("线路配置: line_second_cut=%s, multi_line_array=%s, multi_line_url=%s",
                    lineSecondCut.isEmpty() ? "未配置" : lineSecondCut,
                    multiLineArray.isEmpty() ? "未配置" : multiLineArray,
                    multiLineUrl.isEmpty() ? "未配置" : multiLineUrl));
        }

        if (!multiLineArray.isEmpty() ^ !multiLineUrl.isEmpty()) {
            SpiderDebug.log("警告: multi_line_array 和 multi_line_url 需同时配置才能启用多线模式");
        }
    }

    private String cachedHomePageBody = null;
    
    private long cachedHomePageBodyAt = 0L;
    
    private static final long HOME_CACHE_TTL_MS = 5 * 60 * 1000L;

    private void guessCateManualIfNeeded() {
        try {
            JSONObject cateManual = rule.optJSONObject("cateManual");
            String body = fetchOrCacheHomePageBody();
            // 显式分类配置（含 cat_array 三件套）优先于自动猜测，避免猜测抢先覆盖用户规则
            boolean hasExplicitCate = !getRuleVal("fenlei").isEmpty()
                    || (!getRuleVal("class_name").isEmpty() && !getRuleVal("class_value").isEmpty())
                    || (!getRuleVal("cat_array").isEmpty() && !getRuleVal("cat_title").isEmpty()
                        && !getRuleVal("cat_id").isEmpty());

            if (cateManual == null && !hasExplicitCate) {
                cateManual = guessRuleCateManual(body);
                if (cateManual != null) {
                    rule.put("cateManual", cateManual);
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    /** vod_id 规则是否来自首页自动猜测（非用户显式配置），用于列表/搜索页无命中时的重猜兜底 */
    private boolean guessedVodIdFromHome = false;

    private void guessVodIdIfNeeded(JSONObject list) {
        try {
            if (!list.has("vod_id")) {
                String body = fetchOrCacheHomePageBody();
                JSONArray listVodId = guessRuleVodId(body);
                if (listVodId != null) {
                    guessedVodIdFromHome = true;
                    list.put("vod_id", listVodId);
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    private void guessSearchUrlIfNeeded() {
        try {
            if (!rule.has("search")) {
                String body = fetchOrCacheHomePageBody();
                String url = guessSearchUrlUniversal(body);
                if (url.isEmpty()) url = guessRuleSearchUrl(body);
                if (!url.isEmpty()) {
                    JSONObject searchRule = new JSONObject();
                    searchRule.put("url", url);
                    rule.put("search", searchRule);
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    private String fetchOrCacheHomePageBody() {
        boolean expired = cachedHomePageBody == null
                || SystemClock.elapsedRealtime() - cachedHomePageBodyAt >= HOME_CACHE_TTL_MS;
        if (expired) {
            cachedHomePageBody = fetchHomePageBody();
            cachedHomePageBodyAt = SystemClock.elapsedRealtime();
        }
        return cachedHomePageBody;
    }

    private String fetchHomePageBody() {
        try {
            String body = fetchUrl(rule.getString("homeUrl"), rule.optJSONObject("header"));
            if (body.length() > MAX_HTML_LENGTH) {
                
                body = body.substring(0, MAX_HTML_LENGTH);
                int tagEnd = body.lastIndexOf('>');
                if (tagEnd > 0) body = body.substring(0, tagEnd + 1);
            }
            return body;
        } catch (Exception e) {
            SpiderDebug.log(e);
            return "";
        }
    }

    private void initializeCssRules(JSONObject list) throws JSONException {
        
        boolean jsoupMode = "1".equals(getRuleVal("jsoup_parse", "0"));
        if (!jsoupMode && !hasAnyCssRule()) return;

        SpiderDebug.log("初始化CSS/Jsoup提取规则...");

        initializeListCssRules(list);

        initializeDetailCssRules();

        initializeSearchCssRules();

        initializePlaylistCssRules();

        initializeCategoryCssRules();

        SpiderDebug.log("CSS/Jsoup规则初始化完成");
    }

    private boolean hasAnyCssRule() {
        String[] cssKeys = {"css_selector", "list_css", "detail_css",
                           "search_css", "playlist_css", "cat_css"};
        for (String key : cssKeys) {
            if (rule.has(key)) return true;
        }
        
        return hasCssPrefixInObject(rule.optJSONObject("list"));
    }

    private boolean hasCssPrefixInObject(JSONObject obj) {
        if (obj == null) return false;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object val = obj.get(key);
                if (val instanceof String && JsoupExtractor.isCssRule((String) val)) {
                    return true;
                } else if (val instanceof JSONObject) {
                    if (hasCssPrefixInObject((JSONObject) val)) return true;
                }
            } catch (Exception e) {
                SpiderDebug.log("hasCssPrefixInObject 检查跳过 [" + key + "]: " + e.getMessage());
            }
        }
        return false;
    }

    private void initializeListCssRules(JSONObject list) throws JSONException {
        
        String containerCss = getRuleVal("list_css_container");
        if (!containerCss.isEmpty() && !list.has("css_container")) {
            list.put("css_container", applyOrSelector(containerCss));
        }

        String[][] listCssFields = {
                {"list_name_css", "vod_name"},
                {"list_pic_css", "vod_pic"},
                {"list_id_css", "vod_id"},
                {"list_remarks_css", "vod_remarks"}
        };

        for (String[] pair : listCssFields) {
            String cssVal = getRuleVal(pair[0]);
            if (!cssVal.isEmpty()) {
                String processed = applyOrSelector(cssVal);
                if (JsoupExtractor.isCssRule(processed)) {
                    list.put(pair[1] + "_css", processed);
                    SpiderDebug.log(String.format("列表字段 %s 使用CSS规则: %s", pair[1], processed));
                }
            }
        }

        if (list.has("css_container")) {
            list.put("_use_css_mode", true);
        }
    }

    private void initializeDetailCssRules() throws JSONException {
        JSONObject detail = rule.optJSONObject("detail");
        if (detail == null) detail = new JSONObject();

        String[][] detailCssFields = {
                {"detail_content_css", "vod_content"},
                {"detail_director_css", "vod_director"},
                {"detail_actor_css", "vod_actor"},
                {"detail_type_css", "type_name"},
                {"detail_year_css", "vod_year"},
                {"detail_area_css", "vod_area"},
                {"detail_remarks_css", "vod_remarks"},
                {"detail_pic_css", "vod_pic"},
                {"detail_name_css", "vod_name"}
        };

        boolean hasCss = false;
        for (String[] pair : detailCssFields) {
            String cssVal = getRuleVal(pair[0]);
            if (!cssVal.isEmpty()) {
                String processed = applyOrSelector(cssVal);
                if (JsoupExtractor.isCssRule(processed)) {
                    detail.put(pair[1] + "_css", processed);
                    hasCss = true;
                }
            }
        }

        if (hasCss) {
            detail.put("_use_css_mode", true);
            if (!rule.has("detail")) rule.put("detail", detail);
        }
    }

    private void initializeSearchCssRules() throws JSONException {
        JSONObject search = rule.optJSONObject("search");
        if (search == null) search = new JSONObject();

        String[][] searchCssFields = {
                {"search_name_css", "vod_name"},
                {"search_pic_css", "vod_pic"},
                {"search_id_css", "vod_id"},
                {"search_remarks_css", "vod_remarks"}
        };

        boolean hasCss = false;
        for (String[] pair : searchCssFields) {
            String cssVal = getRuleVal(pair[0]);
            if (!cssVal.isEmpty()) {
                String processed = applyOrSelector(cssVal);
                if (JsoupExtractor.isCssRule(processed)) {
                    search.put(pair[1] + "_css", processed);
                    hasCss = true;
                }
            }
        }

        if (hasCss) {
            search.put("_use_css_mode", true);
            if (!rule.has("search")) rule.put("search", search);
        }
    }

    private void initializePlaylistCssRules() throws JSONException {
        JSONObject playlist = rule.optJSONObject("playlist");
        if (playlist == null) playlist = new JSONObject();

        String fromCss = getRuleVal("from_array_css");
        if (!fromCss.isEmpty() && JsoupExtractor.isCssRule(applyOrSelector(fromCss))) {
            playlist.put("vod_play_from_css", applyOrSelector(fromCss));
        }

        String urlTitleCss = getRuleVal("url_title_css");
        if (!urlTitleCss.isEmpty() && JsoupExtractor.isCssRule(applyOrSelector(urlTitleCss))) {
            playlist.put("vod_play_url_title_css", applyOrSelector(urlTitleCss));
        }

        String urlUrlCss = getRuleVal("url_url_css");
        if (!urlUrlCss.isEmpty() && JsoupExtractor.isCssRule(applyOrSelector(urlUrlCss))) {
            playlist.put("vod_play_url_css", applyOrSelector(urlUrlCss));
        }

        String playArrayCss = getRuleVal("play_array_css");
        if (!playArrayCss.isEmpty() && JsoupExtractor.isCssRule(applyOrSelector(playArrayCss))) {
            playlist.put("play_array_css", applyOrSelector(playArrayCss));
            playlist.put("_use_css_mode", true);
        }

        if (!rule.has("playlist") || playlist.optBoolean("_use_css_mode", false)) {
            rule.put("playlist", playlist);
        }
    }

    private void initializeCategoryCssRules() throws JSONException {
        String catArrayCss = getRuleVal("cat_array_css");
        if (!catArrayCss.isEmpty() && JsoupExtractor.isCssRule(applyOrSelector(catArrayCss))) {
            rule.put("cat_array_css", applyOrSelector(catArrayCss));
        }

        String catTitleCss = getRuleVal("cat_title_css");
        if (!catTitleCss.isEmpty() && JsoupExtractor.isCssRule(applyOrSelector(catTitleCss))) {
            rule.put("cat_title_css", applyOrSelector(catTitleCss));
        }

        String catIdCss = getRuleVal("cat_id_css");
        if (!catIdCss.isEmpty() && JsoupExtractor.isCssRule(applyOrSelector(catIdCss))) {
            rule.put("cat_id_css", applyOrSelector(catIdCss));
        }
    }

    protected JSONArray extractVideoListByCss(String html, JSONObject list) throws JSONException {
        JSONArray videos = new JSONArray();
        Set<String> seenIds = new HashSet<>();

        String containerRule = list.optString("css_container", "");
        if (containerRule.isEmpty()) return videos;

        Map<String, String> fieldRules = new LinkedHashMap<>();
        putIfHasCss(fieldRules, list, "vod_id", "vod_id_css");
        putIfHasCss(fieldRules, list, "vod_name", "vod_name_css");
        putIfHasCss(fieldRules, list, "vod_pic", "vod_pic_css");
        putIfHasCss(fieldRules, list, "vod_remarks", "vod_remarks_css");

        if (fieldRules.isEmpty()) return videos;

        JSONArray items = JsoupExtractor.extractItems(html, containerRule, fieldRules);

        for (int i = 0; i < items.length(); i++) {
            if (videos.length() >= MAX_PAGE_ITEMS) {
                SpiderDebug.log("CSS 列表条目已达单页上限 " + MAX_PAGE_ITEMS + "，截断");
                break;
            }
            JSONObject item = items.getJSONObject(i);
            String vodId = item.optString("vod_id", "");
            if (!vodId.isEmpty()) {
                vodId = getRuleVal("list_prefix") + vodId + getRuleVal("list_suffix");
                // 前后缀必须写回条目再打包，否则 encodeVodId 里仍是未补全的相对链接
                item.put("vod_id", vodId);
            }
            if (vodId.isEmpty() || seenIds.contains(vodId)) continue;

            if (shouldFilter(item.toString(), vodId, list)) continue;

            seenIds.add(vodId);

            supplementMissingFields(item, html);

            item.put("vod_id", encodeVodId(item));

            videos.put(item);
        }

        if (reverseOrder) videos = reverseArray(videos);
        return videos;
    }

    private void putIfHasCss(Map<String, String> map, JSONObject obj,
                               String fieldName, String cssKey) {
        String cssRule = obj.optString(cssKey, "");
        if (!cssRule.isEmpty()) {
            map.put(fieldName, cssRule);
        }
    }

    private void supplementMissingFields(JSONObject item, String html) {
        try {
            
            if (item.optString("vod_pic", "").isEmpty()) {
                String pic = guessValueVodPic(html, 0);
                if (!pic.isEmpty()) {
                    item.put("vod_pic", pic);
                } else {
                    SpiderDebug.log("CSS 提取缺少 vod_pic 字段，猜测兜底失败");
                }
            }
            
            String name = item.optString("vod_name", "");
            if (name.isEmpty()) {
                name = guessValueVodName(html, 0);
                if (!name.isEmpty()) {
                    item.put("vod_name", name);
                } else {
                    SpiderDebug.log("CSS 提取缺少 vod_name 字段，猜测兜底失败");
                }
            }
            
            if (item.optString("vod_remarks", "").isEmpty() && !name.isEmpty()) {
                String remarks = guessValueVodRemarks(html, 0, name);
                if (!remarks.isEmpty()) item.put("vod_remarks", remarks);
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    protected JSONObject extractDetailByCss(String html, JSONObject detail) throws JSONException {
        JSONObject vod = new JSONObject();

        String[][] fields = {
                {"vod_name", "vod_name_css"},
                {"vod_pic", "vod_pic_css"},
                {"type_name", "type_name_css"},
                {"vod_year", "vod_year_css"},
                {"vod_area", "vod_area_css"},
                {"vod_remarks", "vod_remarks_css"},
                {"vod_actor", "vod_actor_css"},
                {"vod_director", "vod_director_css"},
                {"vod_content", "vod_content_css"}
        };

        for (String[] field : fields) {
            String cssKey = field[1];
            if (detail.has(cssKey)) {
                String value = JsoupExtractor.extractSingle(html, detail.getString(cssKey), null);
                if ("vod_pic".equals(field[0]) && !value.isEmpty()) {
                    value = addHttpPrefix(value);
                }
                vod.put(field[0], value);
            }
        }

        return vod;
    }

    protected JSONArray extractSearchResultsByCss(String html, JSONObject search) throws JSONException {
        JSONArray videos = new JSONArray();
        Set<String> seenIds = new HashSet<>();

        String containerRule = search.optString("css_container", "");

        Map<String, String> fieldRules = new LinkedHashMap<>();
        putIfHasCss(fieldRules, search, "vod_id", "vod_id_css");
        putIfHasCss(fieldRules, search, "vod_name", "vod_name_css");
        putIfHasCss(fieldRules, search, "vod_pic", "vod_pic_css");
        putIfHasCss(fieldRules, search, "vod_remarks", "vod_remarks_css");

        if (fieldRules.isEmpty()) {
            
            return extractSearchByCssIndividual(html, search);
        }

        JSONArray items = JsoupExtractor.extractItems(html, containerRule, fieldRules);

        for (int i = 0; i < items.length(); i++) {
            if (videos.length() >= MAX_PAGE_ITEMS) {
                SpiderDebug.log("CSS 列表条目已达单页上限 " + MAX_PAGE_ITEMS + "，截断");
                break;
            }
            JSONObject item = items.getJSONObject(i);
            String vodId = item.optString("vod_id", "");
            if (!vodId.isEmpty()) {
                vodId = getRuleVal("search_prefix") + vodId + getRuleVal("search_suffix");
            }
            if (vodId.isEmpty() || seenIds.contains(vodId)) continue;

            seenIds.add(vodId);
            item.put("vod_id", encodeVodId(item));
            videos.put(item);
        }

        if (reverseOrder) videos = reverseArray(videos);
        return videos;
    }

    private JSONArray extractSearchByCssIndividual(String html, JSONObject search) throws JSONException {
        JSONArray videos = new JSONArray();
        Set<String> seenIds = new HashSet<>();
        int maxCount = Math.min(
                RuleUtils.getSubStringCount(html, search.optString("vod_id_css", "").replace("css:", "")),
                30);

        for (int i = 0; i < maxCount; i++) {
            JSONObject v = new JSONObject();

            if (search.has("vod_id_css")) {
                String idRule = search.getString("vod_id_css").replace("@text", ":eq(" + i + ")@text");
                v.put("vod_id", JsoupExtractor.extractSingle(html, idRule, null));
            }
            if (search.has("vod_name_css")) {
                String nameRule = search.getString("vod_name_css").replace("@text", ":eq(" + i + ")@text");
                v.put("vod_name", JsoupExtractor.extractSingle(html, nameRule, null));
            }
            if (search.has("vod_pic_css")) {
                String picRule = search.getString("vod_pic_css").replace("@attr", ":eq(" + i + ")@attr");
                v.put("vod_pic", addHttpPrefix(JsoupExtractor.extractSingle(html, picRule, null)));
            }
            if (search.has("vod_remarks_css")) {
                String remarksRule = search.getString("vod_remarks_css").replace("@text", ":eq(" + i + ")@text");
                v.put("vod_remarks", JsoupExtractor.extractSingle(html, remarksRule, null));
            }

            if (!v.optString("vod_id", "").isEmpty()) {
                
                String rawId = getRuleVal("search_prefix") + v.optString("vod_id", "") + getRuleVal("search_suffix");
                if (seenIds.contains(rawId)) continue;
                seenIds.add(rawId);
                v.put("vod_id", rawId);
                v.put("vod_id", encodeVodId(v));
                videos.put(v);
            }
        }
        return videos;
    }

    protected JSONArray extractCategoriesByCss(String html) throws JSONException {
        JSONArray classes = new JSONArray();
        Set<String> seenNames = new HashSet<>();

        String titleCss = rule.optString("cat_title_css", "");
        String idCss = rule.optString("cat_id_css", "");

        if (titleCss.isEmpty() || idCss.isEmpty()) return classes;

        List<String> titles = JsoupExtractor.extractList(html, titleCss);
        List<String> ids = JsoupExtractor.extractList(html, idCss);

        int count = Math.min(titles.size(), ids.size());
        for (int i = 0; i < count; i++) {
            String name = titles.get(i).trim();
            String id = ids.get(i).trim();

            if (name.isEmpty() || id.isEmpty()) continue;
            if (seenNames.contains(name)) continue;
            if (isValidCategoryName(name)) {
                seenNames.add(name);
                JSONObject item = new JSONObject();
                item.put("type_name", name);
                item.put("type_id", id);
                classes.put(item);
            }
        }

        return classes;
    }

    protected List<String> extractPlayFromByCss(String html, int expectedSize) {
        List<String> result = new ArrayList<>();
        if (expectedSize <= 0) return result;
        try {
            JSONObject playlist = rule.getJSONObject("playlist");
            String fromCss = playlist.optString("vod_play_from_css", "");
            if (fromCss.isEmpty()) return result;

            List<String> lines = JsoupExtractor.extractList(html, fromCss);
            for (String line : lines) {
                String cleaned = cleanHtml(line).trim();
                if (!cleaned.isEmpty()) {
                    result.add(cleaned);
                }
                if (result.size() >= expectedSize) break;
            }

            if (!result.isEmpty()) {
                result = refinePlayFromNames(result);
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result;
    }

    protected List<String> extractPlayUrlByCss(String html) {
        List<String> result = new ArrayList<>();
        try {
            JSONObject playlist = rule.getJSONObject("playlist");

            String playArrayCss = playlist.optString("play_array_css", "");
            if (!playArrayCss.isEmpty()) {
                result = extractPlayUrlByCssBlocks(html, playArrayCss, playlist);
                if (!result.isEmpty()) return result;
            }

            String urlCss = playlist.optString("vod_play_url_css", "");
            String titleCss = playlist.optString("vod_play_url_title_css", "");
            if (!urlCss.isEmpty()) {
                result = extractPlayUrlByCssSingle(html, urlCss, titleCss);
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result;
    }

    private List<String> extractPlayUrlByCssBlocks(String html, String playArrayCss,
                                                     JSONObject playlist) {
        List<String> blocks = JsoupExtractor.extractList(html, playArrayCss);
        List<String> allEpisodes = new ArrayList<>();

        for (String block : blocks) {
            String urlCss = playlist.optString("vod_play_url_css", "");
            String titleCss = playlist.optString("vod_play_url_title_css", ">");

            List<String> episodes = extractEpisodesFromBlock(block, urlCss, titleCss);
            if (!episodes.isEmpty()) {
                allEpisodes.add(TextUtils.join("#", episodes));
            }
        }
        return allEpisodes;
    }

    private List<String> extractEpisodesFromBlock(String block, String urlCss, String titleCss) {
        List<String> eps = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(block);
            JsoupExtractor.CssRuleInfo urlInfo = JsoupExtractor.parseRule(urlCss);
            JsoupExtractor.CssRuleInfo titleInfo = JsoupExtractor.parseRule(titleCss);

            Elements links = doc.select(urlInfo.selector);
            for (int i = 0; i < links.size(); i++) {
                Element link = links.get(i);
                String href = extractValueByMode(link, urlInfo.mode, urlInfo.attributeName).trim();
                if (href.isEmpty()) continue;

                String title = "";
                if (!titleCss.isEmpty() && titleInfo != null) {
                    
                    Element titleEl = link.selectFirst(titleInfo.selector);
                    if (titleEl == null) titleEl = link.parent().selectFirst(titleInfo.selector);
                    if (titleEl != null) {
                        title = extractValueByMode(titleEl, titleInfo.mode, titleInfo.attributeName).trim();
                    }
                }
                if (title.isEmpty()) title = "第" + (eps.size() + 1) + "集";

                eps.add(title + "$" + addHttpPrefix(href));
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return eps;
    }

    private List<String> extractPlayUrlByCssSingle(String html, String urlCss, String titleCss) {
        List<String> result = new ArrayList<>();
        List<String> urls = JsoupExtractor.extractList(html, urlCss);
        List<String> titles = titleCss.isEmpty()
                ? new ArrayList<>()
                : JsoupExtractor.extractList(html, titleCss);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i).trim();
            if (url.isEmpty()) continue;
            String title = (i < titles.size()) ? titles.get(i).trim() : "第" + (i + 1) + "集";
            if (title.isEmpty()) title = "第" + (i + 1) + "集";
            if (sb.length() > 0) sb.append("#");
            sb.append(title).append("$").append(addHttpPrefix(url));
        }
        if (sb.length() > 0) {
            result.add(sb.toString());
        }
        return result;
    }

    private static String extractValueByMode(Element el, JsoupExtractor.ExtractMode mode, String attrName) {
        return JsoupExtractor.extractValue(el, mode, attrName);
    }

    protected String smartExtractField(String html, String ruleValue, String fallback) {
        if (ruleValue == null || ruleValue.isEmpty()) return fallback;
        if (JsoupExtractor.isCssRule(ruleValue)) {
            String result = JsoupExtractor.extractSingle(html, ruleValue, null);
            return result.isEmpty() ? fallback : result;
        }
        return fallback;
    }

    protected boolean isCssModeEnabled(JSONObject obj) {
        try {
            
            if ("1".equals(getRuleVal("jsoup_parse", "0"))) return true;

            if (obj == null) return false;

            if (obj.optBoolean("_use_css_mode", false)) return true;

            if (obj.has("css_container") && !obj.optString("css_container", "").isEmpty()) return true;

            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.endsWith("_css")) {
                    String val = obj.optString(key, "");
                    if (!val.isEmpty() && JsoupExtractor.isCssRule(val)) return true;
                }
            }
        } catch (Exception e) {
            SpiderDebug.log("isCssModeEnabled error: " + e.getMessage());
        }
        return false;
    }

    private void supplementDetailFieldsFromContext(JSONObject vod, JSONObject vinfo,
                                                   String vid, String detailUrl) throws JSONException {
        
        if (vod.optString("vod_id", "").isEmpty()) {
            vod.put("vod_id", vinfo.optString("vod_id", ""));
        }

        if (vod.optString("vod_name", "").isEmpty() && vinfo.has("vod_name")) {
            vod.put("vod_name", vinfo.getString("vod_name"));
        }

        if (vod.optString("vod_pic", "").isEmpty() && vinfo.has("vod_pic")) {
            String pic = vinfo.getString("vod_pic");
            if ("1".equals(getRuleVal("PicNeedProxy")) && !pic.isEmpty()) {
                pic = fixCover(pic, detailUrl);
            }
            vod.put("vod_pic", addHttpPrefix(pic));
        }

        if (vod.optString("vod_remarks", "").isEmpty() && vinfo.has("vod_remarks")) {
            vod.put("vod_remarks", vinfo.getString("vod_remarks"));
        }
    }

    private String resolveLazyImage(String imgHtml) {
        if (imgHtml == null || imgHtml.isEmpty()) return "";
        try {
            Document doc = Jsoup.parse(imgHtml);
            Element img = doc.selectFirst("img");
            if (img == null) {
                img = doc.selectFirst("[data-original],[data-src],[data-lazy],[data-lazy-src],[data-original-src],[data-img],[exposuresrc],[_src],[lazy-src],[data-bg]");
            }
            if (img == null) return "";
            String configuredAttr = getRuleVal("img_attr");
            if (!configuredAttr.isEmpty()) {
                String val = img.attr(configuredAttr);
                if (val != null && !val.isEmpty()) return val.trim();
            }
            for (String attr : LAZY_IMG_ATTRS) {
                String val = img.attr(attr);
                if (val != null && !val.isEmpty()) return val.trim();
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private String completeImageUrlUniversal(String raw, String baseUrl) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            String lower = raw.toLowerCase();
            for (String scheme : SPECIAL_URL_SCHEMES) {
                if (lower.startsWith(scheme + ":")) return raw;
            }
            if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
            if (lower.startsWith("data:")) return raw;
            if (raw.startsWith("//")) {
                boolean https = baseUrl != null && baseUrl.startsWith("https");
                if (baseUrl == null || baseUrl.isEmpty()) {
                    try { https = rule.getString("homeUrl").startsWith("https"); } catch (Exception ignored) { }
                }
                return (https ? "https:" : "http:") + raw;
            }
            String base = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "";
            if (base.isEmpty()) {
                try { base = rule.getString("homeUrl"); } catch (Exception e) { base = ""; }
            }
            if (base.isEmpty()) return raw;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            int hostRoot = base.indexOf("://");
            if (hostRoot >= 0) hostRoot += 3;
            String url = raw;
            while (url.startsWith("../") || url.startsWith("./")) {
                if (url.startsWith("../")) {
                    url = url.substring(3);
                    int slash = base.lastIndexOf('/');
                    if (hostRoot >= 0 && slash > hostRoot) base = base.substring(0, slash);
                } else {
                    url = url.substring(2);
                }
            }
            return base + (url.startsWith("/") ? url : "/" + url);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return raw;
    }

    public String addHttpPrefix(String url) {
        if (url == null || url.isEmpty()) return "";
        return completeImageUrlUniversal(url, "");
    }

    protected Map<String, String> getHeaders(String url) {
        Map<String, String> headers = new HashMap<>();
        try {
            
            if (!headerMap.isEmpty()) headers.putAll(headerMap);
            
            if (rule.has("header")) {
                Object headerObj = rule.get("header");
                if (headerObj instanceof JSONObject) {
                    JSONObject header = (JSONObject) headerObj;
                    Iterator<String> iter = header.keys();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        headers.put(key, header.getString(key));
                    }
                } else if (headerObj instanceof String) {
                    JSONObject hdr = parseHeader((String) headerObj);
                    Iterator<String> iter = hdr.keys();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        headers.put(key, hdr.getString(key));
                    }
                }
            }
            
            resolveUserAgent(headers);
            
            applyIndependentUaAndReferer(headers);
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
        
        if (!headers.containsKey("User-Agent")) {
            headers.put("User-Agent", Util.CHROME);
        }
        return headers;
    }

    private void resolveUserAgent(Map<String, String> headers) {
        String uaVal = headers.get("User-Agent");
        if ("PC_UA".equals(uaVal)) {
            headers.put("User-Agent", UA_PC);
        } else if ("MOBILE_UA".equals(uaVal)) {
            headers.put("User-Agent", UA_MOBILE);
        }
    }

    private void applyIndependentUaAndReferer(Map<String, String> headers) throws JSONException {
        String ua = rule.optString("User-Agent", "");
        if (!ua.isEmpty()) {
            if ("PC_UA".equals(ua)) {
                headers.put("User-Agent", UA_PC);
            } else if ("MOBILE_UA".equals(ua)) {
                headers.put("User-Agent", UA_MOBILE);
            } else {
                headers.put("User-Agent", ua);
            }
        }
        String referer = rule.optString("Referer", "");
        if (!referer.isEmpty() && referer.startsWith("http")) {
            headers.put("Referer", referer);
        }
    }

    protected JSONObject parseHeader(String headerStr) {
        try {
            if (headerStr.startsWith("{")) {
                return new JSONObject(headerStr);
            }
            
            String normalized = headerStr.trim();
            if ("手机".equals(normalized) || "MOBILE_UA".equals(normalized)) {
                JSONObject hdr = new JSONObject();
                hdr.put("User-Agent", UA_MOBILE);
                return hdr;
            }
            if ("电脑".equals(normalized) || "PC_UA".equals(normalized)) {
                JSONObject hdr = new JSONObject();
                hdr.put("User-Agent", UA_PC);
                return hdr;
            }
            JSONObject hdr = new JSONObject();
            String[] pairs = headerStr.split("#");
            for (String pair : pairs) {
                String[] kv = pair.split("\\$", 2);
                if (kv.length >= 2) {
                    hdr.put(kv[0].trim(), kv[1].trim());
                }
            }
            return hdr;
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
        return new JSONObject();
    }

    private static final Map<String, Pattern> SUB_CONTENT_PATTERN_CACHE = new ConcurrentHashMap<>();

    private static Pattern getSubContentPattern(String escapedStart, String escapedEnd) {
        String regex = escapedStart + "(.*?)" + (escapedEnd.isEmpty() ? "$" : escapedEnd);
        Pattern p = SUB_CONTENT_PATTERN_CACHE.get(regex);
        if (p == null) {
            p = Pattern.compile(regex, Pattern.DOTALL);
            SUB_CONTENT_PATTERN_CACHE.put(regex, p);
        }
        return p;
    }

    protected static List<String> subContent(String content, String startFlag, String endFlag) {
        List<String> result = new ArrayList<>();
        if (startFlag.isEmpty() && endFlag.isEmpty()) {
            result.add(content);
            return result;
        }
        try {
            String escapedStart = escapeExprSpecialWord(startFlag);
            String escapedEnd = escapeExprSpecialWord(endFlag);
            Matcher matcher = getSubContentPattern(escapedStart, escapedEnd).matcher(content);
            while (matcher.find()) {
                result.add(matcher.group(1).trim());
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        if (result.isEmpty()) result.add("");
        return result;
    }

    public static String escapeExprSpecialWord(String keyword) {
        if (!keyword.isEmpty()) {
            String[] specialChars = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
            for (String ch : specialChars) {
                if (keyword.contains(ch)) {
                    keyword = keyword.replace(ch, "\\" + ch);
                }
            }
        }
        return keyword;
    }

    protected String applyOrSelector(String data) {
        if (data == null || !data.contains("||")) return data;
        String[] parts = data.split("\\|\\|");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String resolved = part.trim();
            
            int doubleDash = resolved.indexOf("--");
            if (doubleDash > 0) {
                resolved = resolved.substring(doubleDash + 2);
            }
            if (!resolved.isEmpty()) return resolved;
        }
        return data;
    }

    protected String applySecondCut(String content, String cutRule) {
        if (content == null || content.isEmpty() || cutRule == null || cutRule.isEmpty()) return content;
        cutRule = applyPostProcessors(cutRule);
        String[] parts = cutRule.split("&&");
        if (parts.length == 0) return content;
        int start = 0;
        if (!parts[0].isEmpty()) {
            start = content.indexOf(parts[0]);
            if (start < 0) {
                if (isDebug) SpiderDebug.log("[XBPQ] applySecondCut: prefix not found, returning original content. prefix=" + safeLog(parts[0]));
                return content;
            }
            start += parts[0].length();
        }
        if (parts.length >= 2 && !parts[1].isEmpty()) {
            int end = content.indexOf(parts[1], start);
            if (end < 0) {
                if (isDebug) SpiderDebug.log("[XBPQ] applySecondCut: suffix not found, returning tail. suffix=" + safeLog(parts[1]));
                return content.substring(start);
            }
            return content.substring(start, end).trim();
        }
        return content.substring(start).trim();
    }

    protected String applyPostProcessors(String str) {
        if (str == null || str.isEmpty()) return str;
        try {
            
            List<String[]> procs = new ArrayList<>();
            StringBuilder stripped = new StringBuilder();
            Matcher m = P_PROC_MARK.matcher(str);
            int last = 0;
            while (m.find()) {
                stripped.append(str, last, m.start());
                last = m.end();
                procs.add(new String[]{m.group(1), m.group(2)});
            }
            stripped.append(str, last, str.length());
            String result = stripped.toString();
            for (String[] p : procs) {
                switch (p[0]) {
                    case "替换":
                        
                        for (String pair : p[1].split("#")) {
                            String[] kv = pair.split(">>", 2);
                            if (kv.length != 2 || kv[0].trim().isEmpty()) continue;
                            String oldStr = kv[0].trim();
                            String newStr = kv[1].trim();
                            if (oldStr.contains("*")) {
                                
                                String[] segs = oldStr.split("\\*", -1);
                                StringBuilder rx = new StringBuilder();
                                for (int si = 0; si < segs.length; si++) {
                                    if (!segs[si].isEmpty()) rx.append(Pattern.quote(segs[si]));
                                    if (si < segs.length - 1) {
                                        boolean trailingWildcard = (si == segs.length - 2) && segs[segs.length - 1].isEmpty();
                                        rx.append(trailingWildcard ? ".*" : ".*?");
                                    }
                                }
                                result = result.replaceAll(rx.toString(), Matcher.quoteReplacement(newStr));
                            } else {
                                result = result.replace(oldStr, newStr);
                            }
                        }
                        break;
                    case "包含":
                        result = result.contains(p[1].trim()) ? result : "";
                        break;
                    case "不包含":
                        result = result.contains(p[1].trim()) ? "" : result;
                        break;
                }
                if (result.isEmpty() && !"替换".equals(p[0])) break;
            }
            return result;
        } catch (Exception e) {
            SpiderDebug.log("applyPostProcessors 异常：" + e.getMessage());
            return str;
        }
    }

    protected JSONArray stringCutToLookback(String rule) {
        if (rule == null || rule.isEmpty()) return null;
        String cutRule = applyPostProcessors(rule);
        String[] parts = cutRule.split("&&");
        JSONArray lookback = new JSONArray();
        lookback.put(parts.length >= 1 ? parts[0].trim() : "");       
        lookback.put(parts.length >= 2 && !parts[1].trim().isEmpty() ? parts[1].trim() : 0);  
        lookback.put(0);  
        lookback.put(0);  
        lookback.put(1);  
        return lookback;
    }

    protected void applyStringCutRules(JSONObject target, String ruleKey) {
        if (target == null) return;
        String ruleVal = getRuleVal(ruleKey);
        if (ruleVal.isEmpty()) return;
        String processed = applyOrSelector(ruleVal);
        if (processed.isEmpty()) return;
        JSONArray lookback = stringCutToLookback(processed);
        if (lookback != null) {
            String fieldName = ruleKey.replace("_array", "");
            try {
                if (!target.has(fieldName)) {
                    target.put(fieldName, lookback);
                }
            } catch (JSONException e) {
                SpiderDebug.log(e);
            }
        }
    }

    protected String guessCateManualHtmlString(String body) {
        
        String regex = String.format("<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>\\s*(?:%s)\\s*<",
                TextUtils.join("|", standardCategoryNames));
        // CASE_INSENSITIVE + 单双引号兼容：老旧站点大写标签/属性（<A HREF=…>）与单引号 href
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(body);
        List<HtmlMatchInfo> matchList = new ArrayList<>();
        int matchCount = 0;

        while (m.find()) {
            ++matchCount;
            if (matchCount > MAX_MATCH_COUNT && !matchList.isEmpty()) break;

            HtmlMatchInfo cate = new HtmlMatchInfo();
            cate.init(m);
            cate.group2 = HtmlNodeHelper.trimHtmlString(HtmlNodeHelper.nodeString(body, cate.startPos));
            if (cate.group2.isEmpty()) continue;

            boolean isValid = false;
            for (String name : standardCategoryNames) {
                if (cate.group2.indexOf(name) != -1) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) continue;

            cate.uploads = HtmlNodeHelper.findUpNodes(body, cate.startPos, 3);

            if (!matchList.isEmpty()) {
                if (!matchList.get(0).hasSameUpNode(cate)) {
                    if (matchList.size() > 1) {
                        return HtmlNodeHelper.nodeString(body, matchList.get(0).matchedUpNodePos);
                    }
                    matchList.clear();
                }
            }
            matchList.add(cate);
        }

        if (matchList.size() > 1) {
            return HtmlNodeHelper.nodeString(body, matchList.get(0).matchedUpNodePos);
        }
        return "";
    }

    protected JSONObject guessRuleCateManual(String body) {
        try {
            String str = guessCateManualHtmlString(body);
            if (str.isEmpty()) return new JSONObject();

            String regex = String.format("<a.+?href=[\"'](.+?)[\"'].*?[\"|>](\\s*?\\S+?\\s*?)(\"|<)", TextUtils.join("|", standardCategoryNames));
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher m = pattern.matcher(str);
            List<HtmlMatchInfo> matchList = new ArrayList<>();

            while (m.find()) {
                HtmlMatchInfo cate = new HtmlMatchInfo();
                cate.init(m);
                if (cate.group1.length() < 5) continue;
                cate.group2 = HtmlNodeHelper.trimHtmlString(HtmlNodeHelper.nodeString(str, cate.startPos));
                if (cate.group2.isEmpty()) continue;

                if (!isValidCategoryName(cate.group2)) continue;

                if (!matchList.isEmpty()) {
                    if (!matchList.get(0).findDiffStr(cate, splitFlag)) {
                        continue;
                    }
                }
                matchList.add(cate);
            }

            return buildCateManualFromMatches(matchList);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return new JSONObject();
    }

    private boolean isValidCategoryName(String name) {
        for (String invalid : invalidCategoryNames) {
            if (name.indexOf(invalid) != -1) return false;
        }
        return true;
    }

    private JSONObject buildCateManualFromMatches(List<HtmlMatchInfo> matchList) throws JSONException {
        
        List<Integer> baseIndices = new ArrayList<>();
        for (int i = 0; i < matchList.size(); ++i) {
            for (String name : standardCategoryNames) {
                if (matchList.get(i).group2.indexOf(name) != -1) {
                    baseIndices.add(i);
                    break;
                }
            }
        }

        int baseIndex = 0;
        for (int i = 1; i < baseIndices.size(); ++i) {
            baseIndex = baseIndices.get(0);
            matchList.get(baseIndex).findDiffStr(matchList.get(baseIndices.get(i)), splitFlag);
        }

        JSONObject cateManual = new JSONObject();
        for (int i = 0; i < matchList.size(); ++i) {
            HtmlMatchInfo info = matchList.get(i);
            if (info.diff == null || info.diff.isEmpty()) {
                if (!matchList.get(baseIndex).findDiffStr(info, splitFlag)) continue;
            }

            String name = info.group2;
            String id = info.diff;
            if (id == null || id.isEmpty()) continue;
            if (name == null || name.isEmpty()) continue;

            boolean validId = true;
            for (int k = 0; k < id.length(); ++k) {
                if (splitFlag.indexOf(id.charAt(k)) != -1) {
                    validId = false;
                    break;
                }
            }
            if (!validId) continue;

            if (isValidCategoryName(name) && !cateManual.has(name)) {
                cateManual.put(name, id);
            }
        }

        rule.put("cateManual", cateManual);
        return cateManual;
    }

    private String guessSearchUrlUniversal(String homeHtml) {
        if (homeHtml == null || homeHtml.isEmpty()) return "";
        try {
            Matcher mAction = P_ACTION_ATTR.matcher(homeHtml);
            if (mAction.find()) {
                String url = mAction.group(1);
                Pattern pInput = Pattern.compile("<input[^>]+name=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
                Matcher mInput = pInput.matcher(homeHtml);
                if (mInput.find()) {
                    String wd = mInput.group(1);
                    char sep = url.indexOf('?') == -1 ? '?' : '&';
                    return addHttpPrefix(url + sep + wd + "={wd}");
                }
            }
            Pattern pDataUrl = Pattern.compile("<input[^>]+data-url=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher mDataUrl = pDataUrl.matcher(homeHtml);
            if (mDataUrl.find()) {
                String url = mDataUrl.group(1);
                char sep = url.indexOf('?') == -1 ? '?' : '&';
                return addHttpPrefix(url + sep + "wd={wd}");
            }
            Pattern pJsSearch = Pattern.compile("(?:search|searchkey)\\s*\\(\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher mJs = pJsSearch.matcher(homeHtml);
            if (mJs.find()) {
                String url = mJs.group(1);
                if (url.startsWith("http") || url.startsWith("/")) {
                    char sep = url.indexOf('?') == -1 ? '?' : '&';
                    return addHttpPrefix(url + sep + "wd={wd}");
                }
            }
            for (String path : SEARCH_PATH_PROBES) {
                if (homeHtml.contains(path) || (path.length() > 1 && homeHtml.contains(path.substring(1)))) {
                    return addHttpPrefix(path) + (path.contains("?") ? "&" : "?") + "wd={wd}";
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected String guessRuleSearchUrl(String body) {
        String regex = "<input.+?name=\"(.+?)\"";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(body);
        if (m.find()) {
            String wd = m.group(1);
            for (int i = 1; i < 4; ++i) {
                List<Integer> arr = HtmlNodeHelper.findUpNodes(body, m.start(0), i);
                String r = HtmlNodeHelper.nodeString(body, arr.get(arr.size() - 1));
                Matcher m2 = P_ACTION_ATTR.matcher(r);
                if (m2.find()) {
                    String url = m2.group(1);
                    char separator = url.indexOf('?') == -1 ? '?' : '&';
                    url = addHttpPrefix(url + separator + wd + "={wd}");
                    return url;
                }
            }
        }
        return "";
    }

    public JSONArray guessRuleVodId(String body) {
        try {
            // [^>]*? 同时兼容单/双引号与跨行属性，且不会越过标签边界
            String regex = "<a[^>]*?href=[\"'](.+?)[\"']";
            // CASE_INSENSITIVE：兼容老旧站点的大写标签/属性（<A HREF=…>）
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher m = pattern.matcher(body);
            Map<String, JSONArray> founds = new HashMap<>();
            List<HtmlMatchInfo> matchList = new ArrayList<>();

            while (m.find()) {
                HtmlMatchInfo info = new HtmlMatchInfo();
                info.init(m);
                info.uploads = HtmlNodeHelper.findUpNodes(body, info.startPos, 4);

                if (!matchList.isEmpty()) {
                    if (info.group1.equals(matchList.get(matchList.size() - 1).group1)) continue;
                    if (!matchList.get(matchList.size() - 1).hasSameUpNode(info)) {
                        if (matchList.size() > 1) {
                            processVodIdCandidate(matchList, founds);
                        }
                        matchList.clear();
                    }
                }
                matchList.add(info);
                if (matchList.size() > MAX_MATCH_COUNT) break;
            }

            if (matchList.size() > 5 || (matchList.size() > 1 && founds.isEmpty())) {
                processVodIdCandidate(matchList, founds);
            }

            return selectBestVodIdResult(founds);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    private void processVodIdCandidate(List<HtmlMatchInfo> matchList, Map<String, JSONArray> founds) throws JSONException {
        HtmlMatchInfo info = matchList.get(0);
        if (!info.findDiffStr(matchList.get(1), splitFlag)) return;
        String diff = info.diff == null ? "" : info.diff.trim();
        if (diff.isEmpty()) return;
        boolean numeric = diff.matches("\\d+");
        // 纯数字 id 任意正值均可；slug 型 id（如 /detail/hello-world.html）同样可作候选，
        // 由 selectBestVodIdResult 结合 URL 特征词与出现次数择优
        boolean slug = !numeric && diff.length() <= 60 && !diff.matches(".*\\s.*");
        if (!numeric && !slug) {
            SpiderDebug.log("vod_id 候选异常，跳过 [" + diff + "]");
            return;
        }
        if (numeric && Long.parseLong(diff) <= 0) return;
        String url = info.group1.replace(diff, "{vid}");
        JSONArray arr = buildVodIdArray(url, info, matchList.size());
        updateFoundsMap(founds, url, arr, matchList.size());
    }

    private JSONArray buildVodIdArray(String url, HtmlMatchInfo info, int count) throws JSONException {
        String prefix = url.substring(0, url.indexOf("{vid}"));
        String suffix = url.substring(prefix.length() + "{vid}".length());
        int lookback = info.uploads.indexOf(info.matchedUpNodePos) - 1;
        if (lookback < 1) lookback = 1;

        JSONArray arr = new JSONArray();
        arr.put(prefix);
        arr.put(suffix);
        arr.put(0);
        arr.put(0);
        arr.put(lookback);
        arr.put(count);
        return arr;
    }

    private void updateFoundsMap(Map<String, JSONArray> founds, String url, JSONArray arr, int count) throws JSONException {
        if (!founds.containsKey(url)) {
            founds.put(url, arr);
        } else {
            int newLen = founds.get(url).getInt(5) + count;
            arr.put(5, newLen);
            founds.put(url, arr);
        }
    }

    /** URL 模板含详情页特征词时加权，含列表页特征词时降权，避免分类导航链接压过影片网格 */
    private static final List<String> DETAIL_URL_HINTS = Arrays.asList("detail", "video", "movie", "info");
    private static final List<String> LIST_URL_HINTS = Arrays.asList(
            "type", "list", "class", "sort", "show", "label", "tag", "search", "fenlei", "channel", "category");

    private JSONArray selectBestVodIdResult(Map<String, JSONArray> founds) throws JSONException {
        JSONArray best = null;
        double bestScore = -1;
        String bestUrl = "";
        for (Map.Entry<String, JSONArray> e : founds.entrySet()) {
            JSONArray v = e.getValue();
            String url = e.getKey();
            double score = v.optInt(5, 0);
            String lower = url.toLowerCase();
            for (String hint : DETAIL_URL_HINTS) {
                if (lower.contains(hint)) { score *= 3; break; }
            }
            for (String hint : LIST_URL_HINTS) {
                if (lower.contains(hint)) { score /= 3; break; }
            }
            if (score > bestScore || (score == bestScore && url.compareTo(bestUrl) < 0)) {
                bestScore = score;
                best = v;
                bestUrl = url;
            }
        }
        return best;
    }

    public JSONArray guessRuleVodPlayUrl(String str, String vid) {
        String regex = "href=[\"'](/.+?)[\"']";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(str);
        HtmlMatchInfo info = new HtmlMatchInfo();
        List<String> vec = new ArrayList<>();
        boolean foundStandardFormat = false;

        while (m.find()) {
            String href = m.group(1);
            if (href.length() > 100) continue;
            if (href.indexOf(vid) == -1) continue;

            boolean isStandardFormat = (href.indexOf(vid + "-") != -1);
            if (!isStandardFormat && !vec.isEmpty() && vec.get(vec.size() - 1).length() > href.length()) continue;
            if (isStandardFormat && !foundStandardFormat) vec.clear();
            if (foundStandardFormat && !isStandardFormat) continue;

            info.init(m);
            if (vec.isEmpty()) foundStandardFormat = isStandardFormat;
            vec.add(m.group(1));

            if (vec.size() > 10 && vec.get(vec.size() - 2).length() == href.length()) break;
        }

        if (info.group0 != null) {
            return findPlayListNode(str, info, vid);
        }
        return null;
    }

    private JSONArray findPlayListNode(String str, HtmlMatchInfo info, String vid) {
        for (int i = 1; i < 4; ++i) {
            List<Integer> nodes = HtmlNodeHelper.findUpNodes(str, info.startPos, i);
            int startPos = nodes.get(nodes.size() - 1);
            String nodeStr = HtmlNodeHelper.nodeString(str, startPos);

            if (nodeStr.startsWith("<ul") || nodeStr.startsWith("<div") || i == 3) {
                String prefix = info.group1.substring(0, info.group1.indexOf(vid));
                JSONArray arr = new JSONArray();
                arr.put(prefix);
                arr.put("\"");
                arr.put(0 - prefix.length());
                arr.put(0);
                arr.put(i);
                return arr;
            }
        }
        return null;
    }

    /** 属性值截取：先按双引号、再按单引号，兼容两种书写风格 */
    private static String findAttrValue(String node, String attr, int startPos) {
        try {
            JSONArray vec = new JSONArray();
            vec.put(attr + "=\"");
            vec.put("\"");
            String val = RuleUtils.findSubString(node, startPos, vec);
            if (!val.isEmpty()) return decodeAttrEntities(val);
            vec.put(0, attr + "='");
            vec.put(1, "'");
            val = RuleUtils.findSubString(node, startPos, vec);
            if (!val.isEmpty()) return decodeAttrEntities(val);
            // 非标准模板：属性名全大写（<A HREF=… TITLE=…>，老旧站点），节点解析已大小写无关，这里对齐
            String upper = attr.toUpperCase();
            if (!upper.equals(attr)) {
                vec.put(0, upper + "=\"");
                vec.put(1, "\"");
                val = RuleUtils.findSubString(node, startPos, vec);
                if (val.isEmpty()) {
                    vec.put(0, upper + "='");
                    vec.put(1, "'");
                    val = RuleUtils.findSubString(node, startPos, vec);
                }
            }
            return val.isEmpty() ? "" : decodeAttrEntities(val);
        } catch (org.json.JSONException e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    /** 属性值内常见 HTML 实体解码；&amp; 放最后避免二次解码 */
    private static String decodeAttrEntities(String val) {
        if (val == null || val.indexOf('&') < 0) return val == null ? "" : val;
        return val.replace("&nbsp;", " ")
                .replace("&emsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    public String guessValueVodName(String nodeContent, int startPos) {
        try {
            String val = findAttrValue(nodeContent, "alt", startPos);
            if (val.isEmpty()) {
                val = findAttrValue(nodeContent, "title", startPos);
            }
            if (val.isEmpty()) {
                val = guessNameFromTextContent(nodeContent);
            }
            return cleanCommonPrefixes(val);
        } catch (Exception e) {

        }
        return "";
    }

    /** 疑似画质/更新状态等非片名文本 */
    private static final Pattern P_NAME_NOISE = Pattern.compile(
            "^(HD|SD|BD|4K|蓝光|高清|抢[先鲜]|正片|预告|花絮|枪版|国语|粤语|英语|中字|完结|全集|独家|首发|更新.*|.*更新|第?\\d+\\s*[集期]|[0-9.:\\-\\s]+)$",
            Pattern.CASE_INSENSITIVE);

    private String guessNameFromTextContent(String nodeContent) {
        String all = HtmlNodeHelper.trimHtmlString(nodeContent, "!!!!");
        String[] words = all.split("!!!!");
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String word : words) {
            word = word.trim();
            if (!word.isEmpty() && word.indexOf("更新") == -1 && !P_NAME_NOISE.matcher(word).matches()) {
                frequencyMap.merge(word, 1, Integer::sum);
            }
        }

        String best = "";
        int bestScore = -1;
        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            String word = entry.getKey();
            // 出现次数优先，其次中文词优先、较长词优先，保证并列时结果确定
            int score = entry.getValue() * 100 + (containsCjk(word) ? 50 : 0) + Math.min(word.length(), 30);
            if (score > bestScore) {
                bestScore = score;
                best = word;
            }
        }
        return best;
    }

    private String cleanCommonPrefixes(String val) {
        return val.replace("在线", "").replace("立即", "").replace("观看", "")
                .replace("点播", "").replace("影片", "").replace("信息", "")
                .replace("播放", "").trim();
    }

    public String guessValueVodRemarks(String nodeContent, int startPos, String vodName) {
        try {
            String all = HtmlNodeHelper.trimHtmlString(nodeContent, "!!!!");
            String[] words = all.split("!!!!");
            String remarks = "";
            for (String word : words) {
                String wd = word.trim();
                if (!wd.isEmpty() && wd.indexOf(vodName) == -1) {
                    String separator = remarks.isEmpty() ? "" : ",";
                    String tmp = remarks + separator + wd;
                    if (tmp.length() > 40) break;
                    remarks = tmp;
                }
            }
            return remarks;
        } catch (Exception e) {
            
        }
        return "";
    }

    public String guessValueVodId(String nodeContent) {
        if (nodeContent == null || nodeContent.isEmpty()) return "";
        try {
            return findAttrValue(nodeContent, "href", 0);
        } catch (Exception e) {
            return "";
        }
    }

    public String guessValueVodPic(String nodeContent, int startPos) {
        try {
            // 与 LAZY_IMG_ATTRS 同序：懒加载真实地址属性优先，src 常是占位图必须最后兜底
            for (String attr : LAZY_IMG_ATTRS) {
                String val = findAttrValue(nodeContent, attr, startPos);
                if (!val.isEmpty()) return addHttpPrefix(val);
            }
        } catch (Exception e) {

        }
        return "";
    }

    private boolean isDirectLinkUniversal(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            String current = url;
            for (int round = 0; round < 3; round++) {
                String lower = current.toLowerCase();
                if (!(lower.startsWith("http://") || lower.startsWith("https://"))) return false;
                String videoFilter = getRuleVal("video_filter");
                if (!videoFilter.isEmpty()) {
                    for (String kw : videoFilter.split("#")) {
                        String k = kw.trim().toLowerCase();
                        if (!k.isEmpty() && lower.contains(k)) return false;
                    }
                }
                for (String format : videoFormatList) {
                    if (lower.contains(format)) return true;
                }
                try {
                    URL u = new URL(current);
                    String query = u.getQuery();
                    if (query != null) {
                        String qLower = query.toLowerCase();
                        for (String format : videoFormatList) {
                            String fmt = format.startsWith(".") ? format.substring(1) : format;
                            if (qLower.contains("ext=" + fmt) || qLower.contains("format=" + fmt)) return true;
                        }
                    }
                    String path = u.getPath().toLowerCase();
                    for (String format : videoFormatList) {
                        String fmt = format.startsWith(".") ? format.substring(1) : format;
                        if (path.contains("/" + fmt + "/") || path.endsWith("/" + fmt)) return true;
                    }
                } catch (Exception e) {
                    break;
                }
                String decoded;
                try {
                    decoded = java.net.URLDecoder.decode(current, "UTF-8");
                } catch (Exception e) {
                    break;
                }
                if (decoded.equals(current)) break;
                current = decoded;
            }
            return Util.isVideoFormat(url);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return false;
    }

    private String decodeEscapesDeep(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        try {
            String current = raw;
            Pattern pHex = Pattern.compile("\\\\x([0-9A-Fa-f]{2})");
            for (int round = 0; round < 3; round++) {
                String next = current;
                Matcher um = P_UNICODE_SEQ.matcher(next);
                StringBuffer sb = new StringBuffer();
                while (um.find()) {
                    char c = (char) Integer.parseInt(um.group(2), 16);
                    um.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(c)));
                }
                um.appendTail(sb);
                next = sb.toString();
                Matcher hm = pHex.matcher(next);
                StringBuffer sb2 = new StringBuffer();
                while (hm.find()) {
                    char c = (char) Integer.parseInt(hm.group(1), 16);
                    hm.appendReplacement(sb2, Matcher.quoteReplacement(String.valueOf(c)));
                }
                hm.appendTail(sb2);
                next = sb2.toString();
                next = next.replace("\\/", "/");
                next = next.replace("\\\\", "\\");
                if (next.equals(current)) break;
                current = next;
            }
            return current;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return raw;
    }

    public boolean isVideoFormat(String url) {
        if (url == null) return false;
        String trimmed = url.trim();
        
        String lower = trimmed.toLowerCase();
        if (!(lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("http%3a%2f%2f") || lower.startsWith("https%3a%2f%2f"))) {
            return false;
        }
        if (lower.contains("=http") || lower.contains("=https") ||
            lower.contains("=https%3a%2f") || lower.contains("=http%3a%2f")) {
            return false;
        }
        
        String videoFilter = getRuleVal("video_filter");
        if (!videoFilter.isEmpty()) {
            for (String kw : videoFilter.split("#")) {
                String k = kw.trim().toLowerCase();
                if (!k.isEmpty() && lower.contains(k)) return false;
            }
        }
        for (String format : videoFormatList) {
            if (lower.contains(format)) return true;
        }
        return false;
    }

    @Override
    public boolean manualVideoCheck() throws Exception {
        
        String v = getRuleVal("manualVideoCheck");
        return v.isEmpty() || !"0".equals(v);
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            fetchRule();
            initEnhancedConfig();
            JSONObject result = new JSONObject();
            JSONArray classes = applyCateWhitelist(buildClassList(filter));
            
            classes = insertActionTabs(classes);
            result.put("class", classes);

            // 筛选配置既可能是内联 JSON 对象/JSON 字符串，也可能是远程 URL，
            // 真实规则里还存在 "筛选":"1"/"0"/"" 这类开关写法——此处必须容错，
            // 否则 getJSONObject 抛异常会让整个首页（分类+视频）一起返回空串。
            if (filter) applyHomeFilters(result);
            processFilterData(result, filter);
            processSortFilter(result, filter);

            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    /**
     * 装载首页筛选器。真实规则中「筛选」有四种形态：
     * 1) 内联 JSON 对象（官方写法）；2) JSON 字符串；3) 远程筛选数据 URL；
     * 4) "1"/"0"/"" 之类的开关值（语义不明，忽略即可，绝不能让首页整体失败）。
     */
    private void applyHomeFilters(JSONObject result) {
        if (!rule.has("filter")) return;
        Object val = rule.opt("filter");
        try {
            if (val instanceof JSONObject) {
                result.put("filters", (JSONObject) val);
                return;
            }
            if (!(val instanceof String)) return;
            String s = ((String) val).trim();
            if (s.isEmpty()) return;
            if (s.startsWith("{")) {
                result.put("filters", new JSONObject(s));
                return;
            }
            if (s.startsWith("http")) {
                String body = fetchUrl(s, rule.optJSONObject("header"));
                if (body != null && !body.trim().isEmpty()) {
                    result.put("filters", new JSONObject(body.trim()));
                }
                return;
            }
            // 开关型取值（"1"/"0" 等）：无筛选数据可装载，保持原样
        } catch (Exception e) {
            SpiderDebug.log("首页筛选配置解析失败，已忽略: " + e.getMessage());
        }
    }

    private JSONArray applyCateWhitelist(JSONArray classes) throws JSONException {
        String cfg = getRuleVal("cate_whitelist");
        if (cfg.isEmpty() || classes == null) return classes;
        Set<String> allow = new HashSet<>(Arrays.asList(cfg.split("[,&]")));
        JSONArray filtered = new JSONArray();
        for (int i = 0; i < classes.length(); i++) {
            JSONObject item = classes.optJSONObject(i);
            String name = item == null ? "" : item.optString("type_name", "");
            if (allow.contains(name.trim())) filtered.put(item);
        }
        int dropped = classes.length() - filtered.length();
        if (dropped > 0) SpiderDebug.log("分类白名单: 已过滤 " + dropped + " 个未登记分类");
        return filtered;
    }

    protected JSONArray buildClassList(boolean filter) throws JSONException {
        JSONArray classes = new JSONArray();
        
        if (tryBuildFromJson(classes)) return classes;
        JSONObject cateManual = rule.optJSONObject("cateManual");
        String fenleiExplicit = rule.optString("fenlei", "");

        if (!fenleiExplicit.isEmpty()) {
            cateManual = null;
        }

        if (cateManual == null && fenleiExplicit.isEmpty()) {
            guessCateManualIfNeeded();
            cateManual = rule.optJSONObject("cateManual");
        }

        String catTwice = getRuleVal("cat_twice");
        if (!catTwice.isEmpty() && cateManual == null) {
            String body = fetchHomePageBody();
            body = applySecondCut(body, applyOrSelector(catTwice));
            cateManual = guessRuleCateManual(body);
            if (cateManual != null) rule.put("cateManual", cateManual);
        }

        if (tryBuildFromClassPair(classes)) return classes;
        if (cateManual != null && cateManual.length() > 0) {
            buildClassesFromCateManual(classes, cateManual);
            return classes;
        }
        if (tryBuildFromCatArray(classes, catTwice)) return classes;
        if (tryBuildFromFenlei(classes)) return classes;

        fallbackClassBuild(classes);
        return classes;
    }

    private boolean tryBuildFromClassPair(JSONArray classes) throws JSONException {
        String classNames = rule.optString("class_name", "");
        String classValues = rule.optString("class_value", "");
        if (classNames.isEmpty() || classValues.isEmpty()) return false;

        String[] names = classNames.split("&");
        String[] values = classValues.split("&");
        int len = Math.min(names.length, values.length);
        for (int i = 0; i < len; i++) {
            JSONObject item = new JSONObject();
            
            item.put("type_name", names[i].replace("＆＆", "&"));
            item.put("type_id", values[i].replace("＆＆", "&"));
            classes.put(item);
        }
        return true;
    }

    private void buildClassesFromCateManual(JSONArray classes, JSONObject cateManual) throws JSONException {
        Iterator<String> keys = cateManual.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject item = new JSONObject();
            item.put("type_name", key);
            item.put("type_id", cateManual.getString(key));
            classes.put(item);
        }
    }

    private boolean tryBuildFromCatArray(JSONArray classes, String catTwice) throws JSONException {
        String catArrayRule = getRuleVal("cat_array");
        String catTitleRule = getRuleVal("cat_title");
        String catIdRule = getRuleVal("cat_id");

        if (catArrayRule.isEmpty() || catTitleRule.isEmpty() || catIdRule.isEmpty()) return false;

        String body = fetchHomePageBody();
        if (!catTwice.isEmpty()) {
            body = applySecondCut(body, applyOrSelector(catTwice));
        }

        body = extractCatArrayRegion(body, catArrayRule);

        extractCategoriesFromBody(classes, body, catTitleRule, catIdRule);
        return classes.length() > 0;
    }

    private String extractCatArrayRegion(String body, String catArrayRule) {
        String processed = applyOrSelector(catArrayRule);
        String[] parts = processed.split("&&");
        if (parts.length == 0 || parts[0].isEmpty()) return body;

        int startIdx = body.indexOf(parts[0]);
        if (startIdx < 0) return body;
        body = body.substring(startIdx + parts[0].length());
        // parts[0] 常截在标签属性中间，丢掉首个 '<' 之前的残片（如 `">`），避免污染第一条条目
        if (!body.isEmpty() && body.charAt(0) != '<') {
            int lt = body.indexOf('<');
            if (lt > 0) body = body.substring(lt);
        }

        if (parts.length > 1 && !parts[1].isEmpty()) {
            int endIdx = body.indexOf(parts[1]);
            if (endIdx >= 0) body = body.substring(0, endIdx);
        }
        return body;
    }

    private void extractCategoriesFromBody(JSONArray classes, String body, String titleRule, String idRule) {
        String[] titleParts = titleRule.split("&&");
        String[] idParts = idRule.split("&&");
        String[] items = splitByEndFlag(body, titleParts.length > 1 ? titleParts[1] : "");

        for (String item : items) {
            try {
                String title = extractField(item, titleParts);
                String id = extractField(item, idParts);
                if (!title.isEmpty()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type_name", title);
                    jsonObject.put("type_id", id);
                    classes.put(jsonObject);
                }
            } catch (Exception e) {
                SpiderDebug.log(e);
            }
        }
    }

    private String[] splitByEndFlag(String body, String endFlag) {
        if (endFlag.isEmpty() || !body.contains(endFlag)) {
            return new String[]{body};
        }
        List<String> items = new ArrayList<>();
        int idx = 0;
        while (idx < body.length()) {
            int next = body.indexOf(endFlag, idx);
            if (next < 0) break;
            items.add(body.substring(idx, next + endFlag.length()));
            idx = next + endFlag.length();
        }
        return items.toArray(new String[0]);
    }

    private String extractField(String item, String[] parts) {
        if (parts.length < 2) return item.trim();
        int start = item.indexOf(parts[0]);
        if (start < 0) return "";
        start += parts[0].length();
        int end = item.indexOf(parts[1], start);
        return end > 0 ? item.substring(start, end).trim() : item.substring(start).trim();
    }

    private boolean tryBuildFromFenlei(JSONArray classes) throws JSONException {
        String fenlei = rule.optString("fenlei", "");
        if (fenlei.isEmpty()) return false;

        for (String item : fenlei.split("#")) {
            String[] kv = item.split("\\$");
            if (kv.length >= 2) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type_name", kv[0]);
                jsonObject.put("type_id", kv[1]);
                classes.put(jsonObject);
            }
        }
        // 无 $ 的 fenlei 一个分类都建不出来，返回 false 让 fallbackClassBuild 兜底
        return classes.length() > 0;
    }

    private void fallbackClassBuild(JSONArray classes) throws JSONException {
        if (classes.length() > 0) return;
        String fenlei = rule.optString("fenlei", "");
        if (fenlei.isEmpty()) return;

        String classUrl = rule.optString("class_url", "");
        String cateId = extractCateId(classUrl);
        if (!cateId.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type_name", fenlei);
            jsonObject.put("type_id", cateId);
            classes.put(jsonObject);
        }
    }

    private String extractCateIdMultiStrategy(String url) {
        if (url == null || url.isEmpty()) return "";
        try {
            if (url.matches("\\d+")) return url;
            int qIdx = url.indexOf('?');
            String query = qIdx >= 0 ? url.substring(qIdx + 1) : "";
            if (!query.isEmpty()) {
                for (String param : new String[]{"cid", "id", "typeid", "type_id", "tid"}) {
                    Pattern pQuery = Pattern.compile("(?:^|&)" + param + "=([^&]+)");
                    Matcher mQuery = pQuery.matcher(query);
                    if (mQuery.find()) {
                        String val = mQuery.group(1);
                        if (val.matches("\\d+")) return val;
                        Matcher mEnd = P_EPISODE_NUM.matcher(val);
                        if (mEnd.find()) return mEnd.group(1);
                    }
                }
            }
            Pattern pPathId = Pattern.compile("/(?:id|cid|typeid|type_id)[-/](\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher mPath = pPathId.matcher(url);
            if (mPath.find()) return mPath.group(1);
            Pattern pEndDigit = Pattern.compile("/(\\d+)(?:\\.html?)?(?:[/?#]|$)");
            Matcher mEnd = pEndDigit.matcher(url);
            if (mEnd.find()) return mEnd.group(1);
            Pattern pDashDigit = Pattern.compile("[-_](\\d+)(?:\\.html?)?(?:[/?#&]|$)");
            Matcher mDash = pDashDigit.matcher(url);
            if (mDash.find()) return mDash.group(1);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private String extractCateId(String classUrl) {
        String strategyResult = extractCateIdMultiStrategy(classUrl);
        if (!strategyResult.isEmpty()) return strategyResult;
        if (classUrl.contains("tid=")) {
            int start = classUrl.indexOf("tid=") + 4;
            int end = classUrl.indexOf("&", start);
            return end > start ? classUrl.substring(start, end) : classUrl.substring(start);
        } else if (classUrl.contains("{cateId}") || classUrl.contains("?")) {
            return "1";
        }
        return "";
    }

    private void processFilterData(JSONObject result, boolean filter) throws JSONException {
        if (!filter || !rule.has("filterdata")) return;
        Object filterdata = rule.get("filterdata");
        if (filterdata instanceof JSONObject) {
            result.put("filters", (JSONObject) filterdata);
        } else if (filterdata instanceof String) {
            String furl = (String) filterdata;
            if (furl.startsWith("http")) {
                try {
                    String fjson = OkHttp.string(furl, null);
                    result.put("filters", new JSONObject(fjson));
                } catch (Exception e) {
                    SpiderDebug.log(e);
                }
            }
        }
    }

    private void processSortFilter(JSONObject result, boolean filter) throws JSONException {
        if (!filter) return;
        String sortType = getRuleVal("sort_type");
        String sortValue = getRuleVal("sort_value");
        if (sortType.isEmpty() || sortValue.isEmpty()) return;

        String classUrl = rule.optString("class_url", "");
        if (!classUrl.contains("{by}")) return;

        JSONObject filterObj = new JSONObject();
        JSONObject byItem = new JSONObject();
        byItem.put("key", "by");
        JSONArray listArr = new JSONArray();

        String[] names = sortType.split("&");
        String[] values = sortValue.split("&");
        int len = Math.min(names.length, values.length);
        for (int i = 0; i < len; i++) {
            JSONObject opt = new JSONObject();
            opt.put("n", names[i].trim());
            opt.put("v", values[i].trim());
            listArr.put(opt);
        }
        byItem.put("value", listArr);
        filterObj.put("by", byItem);
        
        JSONObject existingFilters = result.optJSONObject("filters");
        if (existingFilters != null) {
            existingFilters.put("by", byItem);
            result.put("filters", existingFilters);
        } else {
            result.put("filters", filterObj);
        }
    }

    @Override
    public String homeVideoContent() {
        try {
            fetchRule();
            String homeVal = getRuleVal("firstpage");
            
            if (homeVal.isEmpty() && !hotRecommend) return "";

            int maxVideos = DEFAULT_HOME_MAX_VIDEOS;
            List<Pair<String, String>> sections;
            String trimmedHome = homeVal.trim();
            if (trimmedHome.matches("\\d+")) {
                maxVideos = Math.max(1, Integer.parseInt(trimmedHome));
                sections = new ArrayList<>();
            } else {
                sections = parseHomeConfig(homeVal);
            }

            JSONArray classes = null;
            if (!homeVal.isEmpty() || !hotRecommend) {
                String homeContentStr = homeContent(true);
                if (homeContentStr.isEmpty()) return "";
                classes = new JSONObject(homeContentStr).optJSONArray("class");
                if (classes == null) return "";
            }

            if (sections.isEmpty() && classes != null) {
                for (int i = 0; i < classes.length(); i++) {
                    JSONObject c = classes.getJSONObject(i);
                    sections.add(new Pair<>(c.optString("type_name", ""), c.optString("type_id", "")));
                }
            }

            Set<String> seen = new HashSet<>();
            JSONArray allVideos = new JSONArray();
            int count = 0;
            for (int i = 0; i < sections.size() && count < maxVideos; i++) {
                Pair<String, String> sec = sections.get(i);
                JSONArray got = fetchHomeSection(sec.second, seen, maxVideos - count);

                if (got.length() == 0 && !sec.first.isEmpty()) {
                    got = recoverHomeSection(sec.first, classes, seen, maxVideos - count);
                }

                for (int j = 0; j < got.length() && count < maxVideos; j++) {
                    allVideos.put(got.get(j));
                    count++;
                }
            }

            if (reverseOrder) allVideos = reverseArray(allVideos);

            if (hotRecommend) {
                JSONArray hot = fetchHomePageVideos(maxVideos - count);
                for (int j = 0; j < hot.length() && count < maxVideos; j++) {
                    JSONObject v = hot.getJSONObject(j);
                    String key = v.optString("vod_id", "");
                    if (key.isEmpty() || seen.contains(key)) continue;
                    seen.add(key);
                    allVideos.put(v);
                    count++;
                }
            }

            JSONObject result = wrapList(allVideos, null, "1");
            
            if (listDisplay) {
                JSONObject ext = new JSONObject();
                ext.put("listDisplay", "1");
                result.put("ext", ext);
            }
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private List<Pair<String, String>> parseHomeConfig(String homeVal) {
        List<Pair<String, String>> sections = new ArrayList<>();
        for (String item : homeVal.split("#")) {
            String[] kv = item.split("\\$");
            if (kv.length >= 2 && !kv[1].trim().isEmpty()) {
                sections.add(new Pair<>(kv[0].trim(), kv[1].trim()));
            }
        }
        return sections;
    }

    private JSONArray recoverHomeSection(String sectionName, JSONArray classes, Set<String> seen, int cap) throws Exception {
        for (int j = 0; j < classes.length(); j++) {
            JSONObject c = classes.getJSONObject(j);
            if (sectionName.equals(c.optString("type_name", "")) && !c.optString("type_id", "").isEmpty()) {
                return fetchHomeSection(c.getString("type_id"), seen, cap);
            }
        }
        return new JSONArray();
    }

    private JSONArray reverseArray(JSONArray arr) throws JSONException {
        JSONArray reversed = new JSONArray();
        for (int i = arr.length() - 1; i >= 0; i--) {
            reversed.put(arr.get(i));
        }
        return reversed;
    }

    protected JSONArray fetchHomeSection(String tid, Set<String> seen, int cap) {
        JSONArray result = new JSONArray();
        if (tid == null || tid.isEmpty()) return result;
        try {
            String content = categoryContent(tid, "1", false, new HashMap<>());
            if (content.isEmpty()) return result;
            JSONArray list = new JSONObject(content).optJSONArray("list");
            if (list == null) return result;
            for (int i = 0; i < list.length() && result.length() < cap; i++) {
                JSONObject v = list.getJSONObject(i);
                String key = v.optString("vod_id", "");
                if (key.isEmpty() || seen.contains(key)) continue;
                seen.add(key);
                result.put(v);
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result;
    }

    private JSONArray fetchHomePageVideos(int cap) {
        JSONArray result = new JSONArray();
        if (cap <= 0) return result;
        try {
            String body = fetchHomePageBody();
            if (body.isEmpty()) return result;
            JSONObject list = rule.optJSONObject("list");
            if (list == null || !list.has("vod_id")) return result;
            JSONArray videos = extractVideoList(body, list, rule.optString("homeUrl", ""));
            for (int i = 0; i < videos.length() && result.length() < cap; i++) {
                result.put(videos.get(i));
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result;
    }

    protected String categoryUrl(String tid, String pg, boolean filter, Map<String, String> extend) {
        try {
            JSONObject list = this.rule.getJSONObject("list");
            String cateUrl = list.optString(pg, "");
            if (cateUrl.isEmpty()) cateUrl = list.getString("url");

            cateUrl = stripBackticks(cateUrl);

            if (cateUrl.contains(";;")) {
                cateUrl = cateUrl.substring(0, cateUrl.indexOf(";;")).trim();
            }

            if (filter && extend != null && !extend.isEmpty()) {
                for (Iterator<String> it = extend.keySet().iterator(); it.hasNext();) {
                    String key = it.next();
                    String value = extend.get(key);
                    if (!value.isEmpty()) {
                        cateUrl = cateUrl.replace("{" + key + "}", URLEncoder.encode(value, "UTF-8").replace("+", "%20"));
                    }
                }
            }

            int pageSize = parseIntSafely(getRuleVal("page_size"), 20);
            if (pageSize <= 0) pageSize = 20;
            int pageNum = parseIntSafely(shiftStartPage(pg), 1);
            cateUrl = cateUrl.replace("{offset}", String.valueOf((pageNum - 1) * pageSize))
                    .replace("{limit}", String.valueOf(pageSize));
            
            String pgVal = shiftStartPage(pg);
            
            if ("1".equals(getRuleVal("offset_paging")) && !cateUrl.contains("{offset}")) {
                pgVal = String.valueOf((pageNum - 1) * pageSize);
            }
            cateUrl = cateUrl.replace("{cateId}", tid).replace("{catePg}", pgVal);
            
            Matcher matcher = P_BRACE_VAR.matcher(cateUrl);
            StringBuilder sbCate = new StringBuilder();
            int lastEnd = 0;
            while (matcher.find()) {
                String name = matcher.group(1) == null ? "" : matcher.group(1).trim();
                if (!KNOWN_BRACE_KEYS.contains(name)) continue;
                
                sbCate.append(cateUrl, lastEnd, matcher.start());
                lastEnd = matcher.end();
                
                String rest = cateUrl.substring(lastEnd);
                String seg = "/" + name + "/";
                int segIdx = rest.indexOf(seg);
                if (segIdx >= 0 && (segIdx == 0 || rest.charAt(segIdx - 1) == '/')) {
                    lastEnd += segIdx + seg.length();
                }
            }
            if (lastEnd > 0) {
                sbCate.append(cateUrl, lastEnd, cateUrl.length());
                return sbCate.toString();
            }
            return cateUrl;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected String stripBackticks(String url) {
        if (url == null) return "";
        String result = url.trim();
        while (result.startsWith("`")) result = result.substring(1).trim();
        while (result.endsWith("`")) result = result.substring(0, result.length() - 1).trim();
        return result;
    }

    protected String shiftStartPage(String pg) {
        int startPage = parseIntSafely(getRuleVal("startpage"), 1);
        if (startPage < 0) startPage = 0;
        return String.valueOf(parseIntSafely(pg, 1) + startPage - 1);
    }

    protected String shiftSearchPage(String pg) {
        String cfg = getRuleVal("sea_firstpage");
        int startPage = cfg.isEmpty() ? 1 : parseIntSafely(cfg, 1);
        if (startPage < 0) startPage = 0;
        return String.valueOf(parseIntSafely(pg, 1) + startPage - 1);
    }

    protected int parseIntSafely(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        try {
            fetchRule(); 
            JSONObject list = this.rule.getJSONObject("list");
            String url = categoryUrl(tid, pg, filter, extend);

            if ("1".equals(getRuleVal("list_mode"))) {
                JSONArray jsonVideos = extractVideosByJson(httpGetRaw(url),
                        getRuleVal("listjsonlist"), getRuleVal("listjsonid"),
                        getRuleVal("listjsonname"), getRuleVal("listjsonpic"),
                        getRuleVal("listjsonnote"));
                if (jsonVideos.length() > 0) return wrapList(jsonVideos, null, pg).toString();
                SpiderDebug.log("列表 JSON 模式无结果，回退到网页解析");
            }

            String body = fetchUrl(url, list.optJSONObject("header"));

            String endMarker = getRuleVal("page_end_marker");
            if (!endMarker.isEmpty() && body != null && body.contains(endMarker)) {
                SpiderDebug.log("翻页终点标记命中: " + endMarker + "，终止翻页");
                return wrapList(new JSONArray(), null, pg).toString();
            }

            String content = RuleUtils.getRegion(body, list);

            String listTwice = getRuleVal("list_twice");
            if (isCssModeEnabled(list)) {
                SpiderDebug.log("分类列表使用CSS/Jsoup模式提取");
                
                if (!listTwice.isEmpty() && JsoupExtractor.isCssRule(applyOrSelector(listTwice))) {
                    content = JsoupExtractor.cutRegion(content, applyOrSelector(listTwice));
                } else if (!listTwice.isEmpty()) {
                    content = applySecondCut(content, applyOrSelector(listTwice));
                }
                JSONArray cssVideos = extractVideoListByCss(content, list);
                if (cssVideos.length() > 0) {
                    return buildCategoryResult(cssVideos, content, pg);
                }
                SpiderDebug.log("CSS提取无结果，回退到正则模式");
            }

            if (!listTwice.isEmpty()) {
                content = applySecondCut(content, applyOrSelector(listTwice));
            }

            JSONArray videos = extractVideoList(content, list, url);

            return buildCategoryResult(videos, body, pg);
        } catch (Exception e) {
            SpiderDebug.log(e);
            return "";
        }
    }

    private JSONArray extractVideoList(String content, JSONObject list, String url) throws JSONException {
        if (list == null || list.length() == 0) {
            list = buildListFromRules();
        }
        JSONArray videos = scanVideoNodes(content, list, url);

        // 自动猜测模式：首页猜出的链接规则在当前分类页无命中时（模板不一致），用当前页重猜一次
        if (videos.length() == 0 && !content.isEmpty()
                && (guessedVodIdFromHome || list.optJSONArray("vod_id") == null)) {
            JSONArray reGuessed = guessRuleVodId(content);
            if (reGuessed != null && reGuessed.length() >= 5) {
                JSONArray previous = list.optJSONArray("vod_id");
                list.put("vod_id", reGuessed);
                JSONArray retry = scanVideoNodes(content, list, url);
                if (retry.length() > 0) {
                    videos = retry;
                    SpiderDebug.log("自动猜测: 首页链接规则未命中，已按当前列表页重猜成功");
                } else if (previous != null) {
                    list.put("vod_id", previous);
                }
            }
        }

        if (reverseOrder) videos = reverseArray(videos);
        return videos;
    }

    private JSONArray scanVideoNodes(String content, JSONObject list, String url) throws JSONException {
        JSONArray videos = new JSONArray();
        JSONArray lookback = RuleUtils.getLookbackArray(list);
        if (lookback != null) lookback = new JSONArray(lookback.toString());
        Set<String> seenIds = new HashSet<>();
        int pos = 0;

        while (lookback != null) {
            if (videos.length() >= MAX_PAGE_ITEMS) {
                SpiderDebug.log("列表条目已达单页上限 " + MAX_PAGE_ITEMS + "，截断");
                break;
            }
            int matchPos = content.indexOf(lookback.getString(0), pos);
            if (matchPos == -1) break;

            if (insideNoParseBlock(content, matchPos)) {
                pos = matchPos + 1;
                continue;
            }

            NodeExtractionResult result = adjustAndExtractNode(content, matchPos, lookback, list);
            if (result == null) break;

            if (result.endPos <= matchPos) {
                pos = matchPos + 1;
            } else {
                pos = result.endPos;
            }
            String vodId = result.vodId;

            if (vodId.isEmpty()) continue;

            if (!seenIds.contains(vodId)) {

                if (shouldFilter(result.node, vodId, list)) continue;

                seenIds.add(vodId);

                try {
                    JSONObject video = buildVideoObject(result.node, vodId, list, url);
                    videos.put(video);
                } catch (Exception itemEx) {
                    if ("0".equals(getRuleVal("skip_bad_item"))) throw itemEx;
                    SpiderDebug.log("坏条目跳过: vod_id=" + vodId + " err=" + itemEx.getMessage());
                }
            }
        }
        return videos;
    }

    private boolean vodIdRuleWarned = false;

    private void warnMissingVodIdRule() {
        if (vodIdRuleWarned) return;
        vodIdRuleWarned = true;
        SpiderDebug.log("列表未配置「链接」(list_id) 且自动猜测失败，已启用节点级 href 兜底；"
                + "建议显式配置以获取稳定结果");
    }

    private static class NodeExtractionResult {
        String node;
        String vodId;
        int endPos;
    }

    private NodeExtractionResult adjustAndExtractNode(String content, int pos, JSONArray lookback, JSONObject list) throws JSONException {
        List<Integer> urlNodes = null;
        List<Integer> arr = null;
        int blockPos = 0;
        String node = "";
        int lookup = -1;
        int iterations = 0;
        final int MAX_ITERATIONS = 20;

        do {
            
            if (++iterations > MAX_ITERATIONS) {
                SpiderDebug.log(String.format("adjustAndExtractNode 达到最大迭代次数(%d)，当前层级=%d，强制退出", MAX_ITERATIONS, lookback.getInt(4)));
                break;
            }

            arr = HtmlNodeHelper.findUpNodes(content, pos - 1, lookback.getInt(4));
            if (arr.isEmpty()) {
                
                SpiderDebug.log("findUpNodes 未找到祖先节点，跳过当前匹配点");
                return null;
            }
            if (urlNodes == null) {
                urlNodes = arr;
                blockPos = arr.get(arr.size() - 1);
            } else {
                blockPos = RuleUtils.findBlockPos(urlNodes, arr);
            }
            node = HtmlNodeHelper.nodeString(content, blockPos);

            lookup = checkAndAdjustLevel(node, lookup, lookback, urlNodes, blockPos);
            if (lookup < 0) {
                urlNodes = null;
                blockPos = 0;
                node = "";
            }
        } while (lookup < 0);

        NodeExtractionResult result = new NodeExtractionResult();
        result.node = node;
        result.vodId = RuleUtils.findSubString(node, 0, list.optJSONArray("vod_id"));
        
        String listPrefix = getRuleVal("list_prefix");
        String listSuffix = getRuleVal("list_suffix");
        if (!result.vodId.isEmpty() && (!listPrefix.isEmpty() || !listSuffix.isEmpty())) {
            result.vodId = listPrefix + result.vodId + listSuffix;
        }
        
        if (result.vodId.isEmpty() && list.optJSONArray("vod_id") == null) {
            String guessed = guessValueVodId(node);
            if (!guessed.isEmpty()
                    && (!guessValueVodName(node, 0).isEmpty() || !guessValueVodPic(node, 0).isEmpty())) {
                result.vodId = addHttpPrefix(guessed);
                warnMissingVodIdRule();
            }
        }
        result.endPos = blockPos + node.length();
        return result;
    }

    private int checkAndAdjustLevel(String node, int currentLookup, JSONArray lookback,
                                     List<Integer> urlNodes, int blockPos) throws JSONException {
        if (currentLookup >= 0) return currentLookup;

        int level = lookback.getInt(4);
        final int MIN_LEVEL = 1;
        final int MAX_LEVEL = 5;

        if (level < MIN_LEVEL) {
            lookback.put(4, MIN_LEVEL);
            level = MIN_LEVEL;
            SpiderDebug.log(String.format("回看层级低于下限，重置为%d", MIN_LEVEL));
        }
        if (level > MAX_LEVEL) {
            lookback.put(4, MAX_LEVEL);
            level = MAX_LEVEL;
            SpiderDebug.log(String.format("回看层级超过上限，重置为%d", MAX_LEVEL));
        }

        int count = RuleUtils.getSubStringCount(node, lookback.getString(0));
        if (count > 3 && level > MIN_LEVEL) {
            
            lookback.put(4, level - 1);
            SpiderDebug.log(String.format("找到过多的url匹配项(%d)，降低匹配层级为%d", count, level - 1));
            return -2;
        }

        String pic = guessValueVodPic(node, 0);
        String vName = guessValueVodName(node, 0);
        if (pic.isEmpty() || vName.isEmpty()) {
            
            if (level >= MAX_LEVEL) {
                SpiderDebug.log(String.format("回看层级已达上限(%d)仍未找到图片/标题，规则可能不匹配，强制使用当前层级", MAX_LEVEL));
                return level;
            }
            
            lookback.put(4, level + 1);
            SpiderDebug.log(String.format("当前层级未找到(%s)，增加匹配层级为%d", pic.isEmpty() ? "图片" : "标题", level + 1));
            return -2;
        }

        return level;
    }

    private boolean shouldFilter(String node, String vodId, JSONObject list) {
        String filterWord = getRuleVal("filter_word");
        if (filterWord.isEmpty()) return false;

        String vodName = RuleUtils.findSubString(node, 0, list.optJSONArray("vod_name"));
        for (String word : filterWord.split("[,，]")) {
            String trimmed = word.trim();
            if (!trimmed.isEmpty() && (vodId.contains(trimmed) || vodName.contains(trimmed))) {
                return true;
            }
        }
        return false;
    }

    private String guessTitleFromNode(String node) {
        if (node == null || node.isEmpty()) return "";
        try {
            Document doc = Jsoup.parse(node);
            for (Element el : doc.getAllElements()) {
                String title = el.attr("title");
                if (title != null && !title.isEmpty()) return title.trim();
            }
            for (Element el : doc.getAllElements()) {
                String alt = el.attr("alt");
                if (alt != null && !alt.isEmpty()) return alt.trim();
            }
            // 纯文本节点：按噪声过滤+中文优先挑选，避免返回 "HD片名甲" 这类拼接文本
            String picked = guessNameFromTextContent(node);
            if (!picked.isEmpty()) return picked;
            String text = doc.text();
            if (text != null && !text.isEmpty()) return text.trim();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private JSONObject buildVideoObject(String node, String vodId, JSONObject list, String url) throws JSONException {
        JSONObject v = new JSONObject();
        v.put("vod_id", vodId);

        String vodName = RuleUtils.findSubString(node, 0, list.optJSONArray("vod_name"));
        if (vodName.isEmpty()) vodName = guessTitleFromNode(node);
        if (vodName.isEmpty()) vodName = guessValueVodName(node, 0);
        v.put("vod_name", vodName);

        String vodPic = addHttpPrefix(RuleUtils.findSubString(node, 0, list.optJSONArray("vod_pic")));
        if (vodPic.isEmpty()) vodPic = guessValueVodPic(node, 0);
        if (vodPic.isEmpty()) vodPic = addHttpPrefix(resolveLazyImage(node));
        if ("1".equals(getRuleVal("PicNeedProxy")) && !vodPic.isEmpty()) {
            vodPic = fixCover(vodPic, url);
        }
        
        if (vodPic.isEmpty() && !playImage.isEmpty()) {
            vodPic = playImage;
        }
        v.put("vod_pic", vodPic);

        String remarks = RuleUtils.findSubString(node, 0, list.optJSONArray("vod_remarks"));
        if (remarks.isEmpty()) remarks = guessValueVodRemarks(node, 0, vodName);
        v.put("vod_remarks", remarks);

        v.put("vod_id", encodeVodId(v));
        return v;
    }

    private JSONObject buildListFromRules() throws JSONException {
        JSONObject list = new JSONObject();
        String listArray = getRuleVal("list_array");
        if (listArray.isEmpty()) return list;

        String listId = getRuleVal("list_id");
        String listName = getRuleVal("list_name");
        String listPic = getRuleVal("list_pic");
        String listRemarks = getRuleVal("list_remarks");

        JSONArray vodLookback = new JSONArray();
        vodLookback.put(listArray);       
        vodLookback.put(listArray);       
        vodLookback.put(0);               
        vodLookback.put(0);               
        vodLookback.put(3);               

        list.put("vod", vodLookback);

        if (!listId.isEmpty()) {
            JSONArray idRule = new JSONArray();
            idRule.put(listId); idRule.put(listId); idRule.put(0); idRule.put(0);
            list.put("vod_id", idRule);
        }
        if (!listName.isEmpty()) {
            JSONArray nameRule = new JSONArray();
            nameRule.put(listName); nameRule.put(listName); nameRule.put(0); nameRule.put(0);
            list.put("vod_name", nameRule);
        }
        if (!listPic.isEmpty()) {
            JSONArray picRule = new JSONArray();
            picRule.put(listPic); picRule.put(listPic); picRule.put(0); picRule.put(0);
            list.put("vod_pic", picRule);
        }
        if (!listRemarks.isEmpty()) {
            JSONArray remarksRule = new JSONArray();
            remarksRule.put(listRemarks); remarksRule.put(listRemarks); remarksRule.put(0); remarksRule.put(0);
            list.put("vod_remarks", remarksRule);
        }

        SpiderDebug.log("buildListFromRules: list_array=" + listArray + " list_id=" + listId);
        return list;
    }

    private String buildCategoryResult(JSONArray videos, String body, String pg) throws JSONException {
        return wrapList(videos, body, pg).toString();
    }

    public List<String> makeVodPlayFrom(int size) {
        List<String> vec = new ArrayList<>();
        for (int i = 1; i <= size; ++i) {
            vec.add("播放列表" + i);
        }
        return vec;
    }

    private void alignPlaySources(List<String> vodPlayFrom, List<String> vodPlayUrl) {
        int urlSize = vodPlayUrl == null ? 0 : vodPlayUrl.size();
        if (vodPlayFrom == null || urlSize == 0 || vodPlayFrom.size() == urlSize) return;
        if (vodPlayFrom.size() > urlSize) {
            SpiderDebug.log("[播放线路] 线路名(" + vodPlayFrom.size() + ")多于选集组(" + urlSize + ")，已截断对齐");
            vodPlayFrom.subList(urlSize, vodPlayFrom.size()).clear();
        } else {
            SpiderDebug.log("[播放线路] 线路名(" + vodPlayFrom.size() + ")少于选集组(" + urlSize + ")，已补占位线路");
            for (int i = vodPlayFrom.size() + 1; i <= urlSize; i++) {
                vodPlayFrom.add("播放列表" + i);
            }
        }
    }

    public List<String> findVodPlayFrom(String content, int expectedSize) {
        return findVodPlayFrom(content, expectedSize, content);
    }

    public List<String> findVodPlayFrom(String content, int expectedSize, String body) {
        try {
            JSONObject playlist = this.rule.getJSONObject("playlist");
            if (isCssModeEnabled(playlist)) {
                List<String> cssFrom = extractPlayFromByCss(content, expectedSize);
                if (!cssFrom.isEmpty()) {
                    SpiderDebug.log("[播放线路] CSS 模式提取线路名 " + cssFrom.size() + " 条");
                    return cssFrom;
                }
            }
            String fromArrayRule = getRuleVal("from_array");
            if (!fromArrayRule.isEmpty()) {
                List<String> fromArrayResult = tryExtractFromArray(content, expectedSize);
                if (fromArrayResult != null && fromArrayResult.size() > 0) return fromArrayResult;
                if (!body.equals(content)) {
                    fromArrayResult = tryExtractFromArray(body, expectedSize);
                    if (fromArrayResult != null && fromArrayResult.size() > 0) return fromArrayResult;
                }
            }
            if (playlist.has("vod_play_from")) {
                return extractFromRuleConfig(content, playlist, expectedSize);
            }
            return makeVodPlayFrom(expectedSize);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return makeVodPlayFrom(expectedSize);
    }

    private List<String> tryExtractFromArray(String content, int expectedSize) {
        String fromArray = getRuleVal("from_array");
        if (fromArray.isEmpty()) return null;

        String lineSecondCut = getRuleVal("line_second_cut");
        if (!lineSecondCut.isEmpty()) {
            content = applySecondCut(content, applyOrSelector(lineSecondCut));
        }

        String processed = applyOrSelector(fromArray);
        String cutRule = applyPostProcessors(processed);
        String[] parts = cutRule.split("&&");
        if (parts.length < 2) return null;

        List<String> lines = extractLinesByRule(content, parts[0].trim(), parts.length > 1 ? parts[1].trim() : "", expectedSize);
        if (lines.isEmpty()) return null;

        // 如果 from_title 是正则格式且提取的 line 数少于 expectedSize，尝试拆分
        String fromTitle = getRuleVal("from_title");
        if (lines.size() < expectedSize && !fromTitle.isEmpty() && !fromTitle.contains("&&")) {
            String fp = applyPostProcessors(applyOrSelector(fromTitle));
            if (fp.contains("(") || fp.contains("[")) {
                try {
                    Pattern p = Pattern.compile(fp, Pattern.CASE_INSENSITIVE);
                    List<String> allNames = new ArrayList<>();
                    for (String line : lines) {
                        Matcher m = p.matcher(line);
                        while (m.find() && allNames.size() < expectedSize) {
                            allNames.add(cleanHtml(m.group(1).trim()));
                        }
                    }
                    if (allNames.size() > 0) return allNames;
                } catch (Exception ignore) {}
            }
        }

        return refinePlayFromNames(lines);
    }

    private List<String> extractLinesByRule(String content, String start, String end, int maxSize) {
        List<String> lines = new ArrayList<>();
        int linePos = 0;
        while (lines.size() < maxSize) {
            int startPos = content.indexOf(start, linePos);
            if (startPos < 0) break;
            int startIndex = startPos + start.length();
            int endIndex = calculateEndIndex(content, startIndex, end, start);
            lines.add(content.substring(startIndex, endIndex).trim());
            linePos = endIndex + (end.isEmpty() ? 0 : end.length());
        }
        return lines;
    }

    private int calculateEndIndex(String content, int startIndex, String end, String start) {
        if (end.isEmpty()) {
            int nextStart = content.indexOf(start, startIndex);
            int endIndex = nextStart >= 0 ? nextStart : content.length();
            int quote = content.indexOf('"', startIndex);
            if (quote >= 0 && quote < endIndex) endIndex = quote;
            int tag = content.indexOf('<', startIndex);
            if (tag >= 0 && tag < endIndex) endIndex = tag;
            return endIndex;
        } else if (end.startsWith("</") && end.endsWith(">")) {
            String tagName = end.substring(2, end.length() - 1).toLowerCase();
            int depth = 1;
            int pos = startIndex;
            while (depth > 0) {
                int tagStart = content.toLowerCase().indexOf("<" + tagName, pos);
                int tagEnd = content.toLowerCase().indexOf("</" + tagName + ">", pos);
                if (tagEnd < 0) return content.length();
                if (tagStart >= 0 && tagStart < tagEnd) {
                    depth++;
                    pos = tagStart + ("<" + tagName + ">").length();
                } else {
                    depth--;
                    if (depth == 0) return tagEnd;
                    pos = tagEnd + end.length();
                }
            }
            return content.length();
        } else {
            int endIndex = content.indexOf(end, startIndex);
            return endIndex >= 0 ? endIndex : content.length();
        }
    }

    private List<String> extractFromRuleConfig(String content, JSONObject playlist, int expectedSize) throws JSONException {
        List<Pair<Integer, String>> playFromList = new ArrayList<>();
        JSONArray rulePlayFrom = playlist.getJSONArray("vod_play_from");

        for (int i = 0; i < rulePlayFrom.length(); ++i) {
            Object entry = rulePlayFrom.get(i);
            String key = "";
            String alias = "";

            if (entry instanceof String) {
                key = alias = (String) entry;
            } else if (entry instanceof JSONArray) {
                JSONArray item = (JSONArray) entry;
                key = alias = item.getString(0);
                if (item.length() > 1) alias = item.getString(1);
            } else {
                return makeVodPlayFrom(expectedSize);
            }

            int position = content.indexOf(key);
            if (position == -1) continue;
            playFromList.add(new Pair<>(position, alias));
        }

        if (playFromList.size() != expectedSize) return makeVodPlayFrom(expectedSize);

        Collections.sort(playFromList, Comparator.comparingInt(pair -> pair.first));

        List<String> result = new ArrayList<>();
        for (Pair<Integer, String> pair : playFromList) {
            result.add(pair.second);
        }
        return result;
    }

    protected List<String> refinePlayFromNames(List<String> lines) {
        try {
            String fromTitle = getRuleVal("from_title");
            if (fromTitle.isEmpty()) return lines;

            String processed = applyPostProcessors(applyOrSelector(fromTitle));
            String[] tp = processed.split("&&");
            String ts = tp.length > 0 ? tp[0].trim() : "";
            String te = tp.length > 1 ? tp[1].trim() : "";
            if (ts.isEmpty()) return lines;

            List<String> refined = new ArrayList<>();
            // 如果 ts 包含正则捕获组模式（无 && 分隔符，但含 ( ) 和 [ ]），使用正则提取所有匹配
            if (!processed.contains("&&") && (ts.contains("(") || ts.contains("["))) {
                try {
                    Pattern p = Pattern.compile(ts, Pattern.CASE_INSENSITIVE);
                    boolean matchedAny = false;
                    for (String line : lines) {
                        Matcher m = p.matcher(line);
                        String val = line;
                        if (m.find()) {
                            val = cleanHtml(m.group(1).trim());
                            matchedAny = true;
                        }
                        refined.add(val.isEmpty() ? line : val);
                    }
                    // 如果所有 line 都没匹配到，回退到单行模式
                    if (!matchedAny) {
                        for (String line : lines) refined.add(line);
                    }
                } catch (Exception ignore) {
                    for (String line : lines) refined.add(line);
                }
            } else {
                for (String line : lines) {
                    String val = line;
                    int a = line.indexOf(ts);
                    if (a >= 0) {
                        a += ts.length();
                        int b = te.isEmpty() ? line.length() : line.indexOf(te, a);
                        if (b < 0) b = line.length();
                        val = cleanHtml(line.substring(a, b));
                    }
                    refined.add(val.isEmpty() ? line : val);
                }
            }
            return refined;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return lines;
    }

    private static int countPlayEpisodes(List<String> lines) {
        int n = 0;
        if (lines == null) return 0;
        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;
            n += line.split("#").length;
        }
        return n;
    }

    private void logPlayStrategy(String strategy, long startNanos, List<String> lines) {
        try {
            SpiderDebug.log("[播放提取] 策略=" + strategy
                    + " 线路=" + (lines == null ? 0 : lines.size())
                    + " 选集=" + countPlayEpisodes(lines)
                    + " 耗时=" + (System.nanoTime() - startNanos) / 1000000 + "ms");
        } catch (Exception ignored) {
        }
    }

    public List<String> findVodPlayUrl(String content) {
        List<String> playUrl = new ArrayList<>();
        try {
            JSONObject playlist = this.rule.getJSONObject("playlist");
            int sort = playlist.optInt("sort", 0);
            long startNanos = System.nanoTime();

            if (isCssModeEnabled(playlist)) {
                List<String> cssResult = extractPlayUrlByCss(content);
                if (cssResult != null && !cssResult.isEmpty()) {
                    logPlayStrategy("css", startNanos, cssResult);
                    return cssResult;
                }
            }

            List<String> fromLinkResult = tryFromLinkMode(content);
            if (fromLinkResult != null) {
                logPlayStrategy("from_link", startNanos, fromLinkResult);
                return fromLinkResult;
            }

            List<String> multiLineResult = tryMultiLineMode(content);
            if (multiLineResult != null) {
                logPlayStrategy("multi_line", startNanos, multiLineResult);
                return multiLineResult;
            }

            List<String> playArrayResult = tryPlayArrayMode(content, playlist, sort);
            if (playArrayResult != null) {
                logPlayStrategy("play_array", startNanos, playArrayResult);
                return playArrayResult;
            }

            List<String> playArrayDefault = tryPlayArrayDefaultMode(content, sort);
            if (playArrayDefault != null) {
                logPlayStrategy("play_array_default", startNanos, playArrayDefault);
                return playArrayDefault;
            }

            if (playUrl.isEmpty()) {
                JSONArray playLookback = playlist.optJSONArray("vod_play_url");
                if (playLookback != null) {
                    
                    String url = "";
                    int scan = 0;
                    for (int i = 0; i < MAX_FALLBACK_SCAN && scan < content.length(); i++) {
                        String one = RuleUtils.findSubString(content, scan, playLookback);
                        if (one.isEmpty()) break;
                        int at = content.indexOf(one, scan);
                        scan = at >= 0 ? at + one.length() : scan + 1;
                        if (isPlausibleEpisodeUrl(one)) {
                            url = one;
                            break;
                        }
                    }
                    if (!url.isEmpty()) {
                        
                        int urlEnd = content.indexOf(url) + url.length();
                        String[] titleBounds = getTitleBounds();
                        String title = extractEpisodeTitle(content, urlEnd, titleBounds);
                        if (title.isEmpty() || title.contains("\n")) title = "第1集";
                        playUrl.add(title + "$" + addHttpPrefix(url));
                    }
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return playUrl;
    }

    private List<String> tryMultiLineMode(String content) throws JSONException {
        String multiLineArray = getRuleVal("multi_line_array");
        String multiLineUrl = getRuleVal("multi_line_url");
        if (multiLineArray.isEmpty() || multiLineUrl.isEmpty()) return null;

        String multiLineTwice = getRuleVal("multi_line_twice");
        String multiLinePrefix = getRuleVal("multi_line_prefix");
        String multiLineSuffix = getRuleVal("multi_line_suffix");

        String processedBody = content;
        if (!multiLineTwice.isEmpty()) {
            processedBody = applySecondCut(content, applyOrSelector(multiLineTwice));
        }

        return extractMultiLines(processedBody, multiLineArray, multiLineUrl, multiLinePrefix, multiLineSuffix);
    }

    private List<String> extractMultiLines(String body, String arrayRule, String urlRule,
                                            String prefix, String suffix) throws JSONException {
        String[] arrayParts = applyOrSelector(arrayRule).split("&&");
        String[] urlParts = applyOrSelector(urlRule).split("&&");
        if (arrayParts.length < 2 || urlParts.length < 2) return null;

        List<String> lines = new ArrayList<>();
        int linePos = 0;
        while (lines.size() < 10) {
            int aStart = body.indexOf(arrayParts[0].trim(), linePos);
            if (aStart < 0) break;
            int aEnd = body.indexOf(arrayParts[1].trim(), aStart + arrayParts[0].length());
            if (aEnd < 0) break;

            String lineContent = body.substring(aStart + arrayParts[0].length(), aEnd);
            int uStart = lineContent.indexOf(urlParts[0].trim());
            if (uStart < 0) { linePos = aEnd + arrayParts[1].length(); continue; }

            String afterUrlStart = lineContent.substring(uStart + urlParts[0].length());
            int uEnd = afterUrlStart.indexOf(urlParts[1].trim());
            if (uEnd < 0) { linePos = aEnd + arrayParts[1].length(); continue; }

            lines.add(prefix + afterUrlStart.substring(0, uEnd) + suffix);
            linePos = aEnd + arrayParts[1].length();
        }

        if (lines.isEmpty()) return null;
        List<String> result = new ArrayList<>();
        result.add(TextUtils.join("#", lines));
        return result;
    }

    private List<String> tryFromLinkMode(String content) throws JSONException {
        String fromArray = getRuleVal("from_array");
        String lineUrl = getRuleVal("multi_line_url");
        String multiLineArray = getRuleVal("multi_line_array");
        if (fromArray.isEmpty() || lineUrl.isEmpty() || !multiLineArray.isEmpty()) return null;

        String processedBody = content;
        String lineSecondCut = getRuleVal("line_second_cut");
        if (!lineSecondCut.isEmpty()) {
            processedBody = applySecondCut(content, applyOrSelector(lineSecondCut));
        }

        String[] parts = applyPostProcessors(applyOrSelector(fromArray)).split("&&");
        if (parts.length < 2) return null;
        String start = parts[0].trim();
        String end = parts.length > 1 ? parts[1].trim() : "";

        List<String> lineRegions = extractLinesByRule(processedBody, start, end, 10);
        if (lineRegions.isEmpty()) return null;

        JSONObject playlist = rule.optJSONObject("playlist");
        int sort = playlist != null ? playlist.optInt("sort", 0) : 0;
        List<String> lines = new ArrayList<>();
        for (String region : lineRegions) {
            if (lines.size() >= 10) break;
            String url = extractSingleUrl(region, lineUrl);
            if (url.isEmpty()) continue;
            url = addHttpPrefix(url);

            try {
                String lineBody = fetchUrl(url, playlist != null ? playlist.optJSONObject("header") : null);
                lineBody = RuleUtils.getRegion(lineBody, playlist);
                String playTwice = getRuleVal("play_twice");
                if (!playTwice.isEmpty()) {
                    lineBody = applySecondCut(lineBody, applyOrSelector(playTwice));
                }
                List<String> eps = tryPlayArrayMode(lineBody, playlist, sort);
                if (eps != null && !eps.isEmpty()) {
                    lines.add(TextUtils.join("#", eps));
                }
            } catch (Exception e) {
                SpiderDebug.log(safeLog("线路链接模式拉取失败: " + url + " -> " + e.getMessage()));
            }
        }
        return lines.isEmpty() ? null : lines;
    }

    private String extractSingleUrl(String region, String urlRule) {
        try {
            String[] up = applyPostProcessors(applyOrSelector(urlRule)).split("&&", 2);
            if (up.length < 2) return "";
            String s = up[0].trim();
            String e = up[1].trim();
            if (s.isEmpty()) return "";
            int a = region.indexOf(s);
            if (a < 0) return "";
            int b = e.isEmpty() ? region.indexOf('"', a + s.length())
                    : region.indexOf(e, a + s.length());
            if (b < 0) b = region.length();
            return region.substring(a + s.length(), b).trim();
        } catch (Exception ex) {
            return "";
        }
    }

    private List<String> tryPlayArrayMode(String content, JSONObject playlist, int sort) throws JSONException {
        List<String> tmpPlayUrl = new ArrayList<>();
        String playArrayRule = getRuleVal("play_array");
        String urlUrlRule = getRuleVal("url_url");
        if (playArrayRule.isEmpty() || urlUrlRule.isEmpty() ||
            !playArrayRule.contains("&&") || !urlUrlRule.contains("&&")) return null;

        String[] pa = applyPostProcessors(applyOrSelector(playArrayRule)).split("&&", 2);
        String[] ua = applyPostProcessors(applyOrSelector(urlUrlRule)).split("&&", 2);
        String listStart = pa[0].trim();
        String listEnd = pa.length > 1 ? pa[1].trim() : "</ul>";
        String hrefStart = ua[0].trim();
        String hrefEnd = ua.length > 1 ? ua[1].trim() : "\"";

        String[] titleBounds = getTitleBounds();
        
        String urlArrayRule = getRuleVal("url_array");
        String[] itemBounds = parseItemBounds(urlArrayRule);
        int listPos = 0;
        int blockCount = 0;

        boolean filterConfigured = !getRuleVal("episode_filter").isEmpty();

        while (tmpPlayUrl.size() < MAX_PLAY_LINES) {
            int ls = content.indexOf(listStart, listPos);
            if (ls < 0) break;
            int le = content.indexOf(listEnd, ls + listStart.length());
            if (le < 0) break;
            String block = content.substring(ls, le);
            listPos = le + listEnd.length();
            blockCount++;

            if (block.contains("</script")) continue;

            List<String> eps = extractEpisodesUniversal(block, playlist);
            if (eps.isEmpty()) {
                eps = itemBounds == null
                        ? extractEpisodes(block, hrefStart, hrefEnd, titleBounds, sort)
                        : extractEpisodesByItem(block, itemBounds[0], itemBounds[1],
                                                hrefStart, hrefEnd, titleBounds, sort);
            }
            if (eps.isEmpty() && !filterConfigured) {
                eps = itemBounds == null
                        ? extractEpisodes(block, hrefStart, hrefEnd, titleBounds, sort, true)
                        : extractEpisodesByItem(block, itemBounds[0], itemBounds[1],
                                                hrefStart, hrefEnd, titleBounds, sort, true);
            }
            if (!eps.isEmpty()) {
                tmpPlayUrl.add(TextUtils.join("#", eps));
            }
        }

        if (!tmpPlayUrl.isEmpty()) {
            SpiderDebug.log("playArray: blocks=" + blockCount + " episodes=" + tmpPlayUrl.size());
            return tmpPlayUrl;
        }
        return null;
    }

    private static final int MAX_FALLBACK_SCAN = 50;

    private static final List<String> STATIC_RESOURCE_EXTS = Arrays.asList(
            ".ico", ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg",
            ".webp", ".woff", ".woff2", ".ttf", ".eot", ".xml", ".txt");

    private static boolean isPlausibleEpisodeUrl(String url) {
        if (url == null) return false;
        String u = url.trim();
        if (u.isEmpty()) return false;
        String lower = u.toLowerCase();
        if (lower.startsWith("#") || lower.startsWith("javascript:")
                || lower.startsWith("mailto:") || lower.contains("favicon")) {
            return false;
        }
        int q = lower.indexOf('?');
        String path = q > 0 ? lower.substring(0, q) : lower;
        for (String ext : STATIC_RESOURCE_EXTS) {
            if (path.endsWith(ext)) return false;
        }
        return true;
    }

    private List<String> tryPlayArrayDefaultMode(String content, int sort) throws JSONException {
        String playArrayRule = getRuleVal("play_array");
        if (playArrayRule.isEmpty() || !playArrayRule.contains("&&")) return null;
        
        if (!getRuleVal("url_url").isEmpty()) return null;

        String[] pa = applyPostProcessors(applyOrSelector(playArrayRule)).split("&&", 2);
        String listStart = pa[0].trim();
        String listEnd = pa.length > 1 ? pa[1].trim() : "</ul>";
        String[] titleBounds = getTitleBounds();

        List<String> lines = new ArrayList<>();
        int listPos = 0;
        while (lines.size() < 10) {
            int ls = content.indexOf(listStart, listPos);
            if (ls < 0) break;
            int le = content.indexOf(listEnd, ls + listStart.length());
            if (le < 0) break;
            String block = content.substring(ls, le);
            listPos = le + listEnd.length();
            
            if (block.contains("</script")) continue;
            List<String> eps = extractEpisodes(block, "href=\"", "\"", titleBounds, sort);
            if (!eps.isEmpty()) lines.add(TextUtils.join("#", eps));
        }
        if (lines.isEmpty()) return null;
        SpiderDebug.log("播放链接(url_url) 未配置，已按默认 href 规则兜底解析 "
                + lines.size() + " 条线路");
        return lines;
    }

    private String[] getTitleBounds() {
        String urlTitleRule = getRuleVal("url_title");
        if (!urlTitleRule.isEmpty() && urlTitleRule.contains("&&")) {
            return applyPostProcessors(applyOrSelector(urlTitleRule)).split("&&", 2);
        }
        return DEFAULT_TITLE_BOUNDS.clone();
    }

    private String[] parseItemBounds(String urlArrayRule) {
        if (urlArrayRule.isEmpty()) return null;
        String[] up = applyPostProcessors(applyOrSelector(urlArrayRule)).split("&&", 2);
        String itemStart = up[0].trim();
        if (itemStart.isEmpty()) return null;
        String itemEnd = up.length > 1 ? up[1].trim() : "";
        if (itemEnd.isEmpty()) itemEnd = "</a>";
        return new String[]{itemStart, itemEnd};
    }

    private List<String> extractEpisodesByItem(String block, String itemStart, String itemEnd,
                                               String hrefStart, String hrefEnd,
                                               String[] titleBounds, int sort) {
        return extractEpisodesByItem(block, itemStart, itemEnd, hrefStart, hrefEnd,
                titleBounds, sort, false);
    }

    private List<String> extractEpisodesByItem(String block, String itemStart, String itemEnd,
                                               String hrefStart, String hrefEnd,
                                               String[] titleBounds, int sort, boolean ignoreFilter) {
        List<String> eps = new ArrayList<>();
        int p = 0;
        while (true) {
            int s = block.indexOf(itemStart, p);
            if (s < 0) break;
            int e = block.indexOf(itemEnd, s + itemStart.length());
            if (e < 0) break;
            // item 需包含 itemEnd 尾标记：titleEnd 常与 itemEnd 相同（如 </a>），截掉会导致标题永远取不到
            String item = block.substring(s, e + itemEnd.length());
            p = e + itemEnd.length();

            int hs = item.indexOf(hrefStart);
            if (hs < 0) continue;
            int he0 = hs + hrefStart.length();
            int he = item.indexOf(hrefEnd, he0);
            if (he < 0) continue;
            String href = item.substring(he0, he).trim();
            if (href.contains("&amp;")) href = href.replace("&amp;", "&");
            if (!ignoreFilter && !matchEpisodeFilter(href)) continue;

            String title = extractEpisodeTitle(item, he, titleBounds);
            if (title.isEmpty()) title = extractEpisodeTitleUniversal(item, he, titleBounds);
            if (title.contains("展开全部")) continue;
            if (title.isEmpty()) title = "第" + (eps.size() + 1) + "集";
            eps.add(title + "$" + addHttpPrefix(href));
        }
        if (sort != 0) Collections.reverse(eps);
        return eps;
    }

    private List<String> extractEpisodesUniversal(String block, JSONObject playlist) {
        List<String> eps = new ArrayList<>();
        if (block == null || block.isEmpty()) return eps;
        try {
            Matcher mDataUrl = P_DATA_URL_ATTR.matcher(block);
            int idx = 0;
            while (mDataUrl.find() && eps.size() < MAX_MATCH_COUNT) {
                String url = mDataUrl.group(1);
                String title = mDataUrl.group(2);
                if (title != null) title = cleanHtml(title).trim();
                if (title == null || title.isEmpty()) title = "第" + (idx + 1) + "集";
                eps.add(title + "$" + addHttpPrefix(url));
                idx++;
            }
            if (!eps.isEmpty()) return eps;
            Matcher mJsPlayer = P_JS_PLAYER_CALL.matcher(block);
            idx = 0;
            while (mJsPlayer.find() && eps.size() < MAX_MATCH_COUNT) {
                String url = mJsPlayer.group(1);
                int end = mJsPlayer.end();
                String title = extractEpisodeTitleUniversal(block, end, DEFAULT_TITLE_BOUNDS);
                if (title.isEmpty()) title = "第" + (idx + 1) + "集";
                eps.add(title + "$" + addHttpPrefix(url));
                idx++;
            }
            if (!eps.isEmpty()) return eps;
            Matcher mOnclick = P_ONCLICK_PLAYER.matcher(block);
            idx = 0;
            while (mOnclick.find() && eps.size() < MAX_EPISODE_COUNT) {
                String url = mOnclick.group(1);
                int end = mOnclick.end();
                String title = extractEpisodeTitleUniversal(block, end, DEFAULT_TITLE_BOUNDS);
                if (title.isEmpty()) title = "第" + (idx + 1) + "集";
                eps.add(title + "$" + addHttpPrefix(url));
                idx++;
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return eps;
    }

    private String extractEpisodeTitleUniversal(String block, int hrefEnd, String[] bounds) {
        try {
            String title = extractEpisodeTitle(block, hrefEnd, bounds);
            if (!title.isEmpty()) return title;
            int searchStart = Math.max(0, hrefEnd);
            if (searchStart < block.length()) {
                String region = block.substring(searchStart, Math.min(block.length(), searchStart + 200));
                Matcher mSpan = P_EPISODE_SPAN.matcher(region);
                if (mSpan.find()) {
                    String t = cleanHtml(mSpan.group(1)).trim();
                    if (!t.isEmpty()) return t;
                }
            }
            if (hrefEnd < block.length()) {
                String tail = block.substring(hrefEnd, Math.min(block.length(), hrefEnd + 100));
                Matcher mNum = P_EPISODE_NUM.matcher(tail);
                if (mNum.find()) return mNum.group(1);
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private List<String> extractEpisodes(String block, String hrefStart, String hrefEnd,
                                          String[] titleBounds, int sort) {
        return extractEpisodes(block, hrefStart, hrefEnd, titleBounds, sort, false);
    }

    private List<String> extractEpisodes(String block, String hrefStart, String hrefEnd,
                                          String[] titleBounds, int sort, boolean ignoreFilter) {
        List<String> eps = new ArrayList<>();
        int hp = 0;
        while (true) {
            int hs = block.indexOf(hrefStart, hp);
            if (hs < 0) break;
            int he0 = hs + hrefStart.length();
            int he = block.indexOf(hrefEnd, he0);
            if (he < 0) break;
            String href = block.substring(he0, he).trim();
            
            if (href.contains("&amp;")) href = href.replace("&amp;", "&");
            hp = he + hrefEnd.length();
            if (!ignoreFilter && !matchEpisodeFilter(href)) continue;

            String title = extractEpisodeTitleUniversal(block, he, titleBounds);
            if (title.contains("展开全部")) continue;
            if (title.isEmpty()) title = "第" + (eps.size() + 1) + "集";
            eps.add(title + "$" + addHttpPrefix(href));
        }
        if (sort != 0) Collections.reverse(eps);
        return eps;
    }

    private boolean matchEpisodeFilter(String href) {
        if (href == null || href.isEmpty()) return false;
        String cfg = getRuleVal("episode_filter");
        if (cfg.equals("0")) return true;
        if (!cfg.isEmpty()) {
            for (String kw : cfg.split("#")) {
                kw = kw.trim();
                if (!kw.isEmpty() && href.contains(kw)) return true;
            }
            return false;
        }
        
        return href.contains("/play/") || href.contains("vodplay");
    }

    private String extractEpisodeTitle(String block, int hrefEnd, String[] titleBounds) {
        String titleStart = titleBounds[0];
        String titleEnd = titleBounds[1];
        int ts = block.indexOf(titleStart, hrefEnd);
        if (ts >= 0 && ts < hrefEnd + 120) {
            int te = block.indexOf(titleEnd, ts + titleStart.length());
            if (te > ts) return cleanHtml(block.substring(ts + titleStart.length(), te));
        }
        return "";
    }

    private String guessDetailRegionUniversal(String body) {
        if (body == null || body.isEmpty()) return "";
        try {
            Matcher m = P_DETAIL_FIELD_FUZZY.matcher(body);
            List<Integer> positions = new ArrayList<>();
            while (m.find() && positions.size() < 5) {
                positions.add(m.start());
            }
            if (positions.size() >= 2) {
                List<Integer> upNodes = HtmlNodeHelper.findUpNodes(body, positions.get(0), 5);
                for (int upPos : upNodes) {
                    String node = HtmlNodeHelper.nodeString(body, upPos);
                    int containCount = 0;
                    for (int pos : positions) {
                        if (pos >= upPos && pos < upPos + node.length()) containCount++;
                    }
                    if (containCount >= 2) return node;
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected String guessDetailContentRegion(String body) {
        String regex = String.format(">\\s*?(%s)|(%s)", TextUtils.join("|", detailFieldNames), TextUtils.join("：|", detailFieldNames));
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(body);
        List<HtmlMatchInfo> matchList = new ArrayList<>();

        while (m.find()) {
            HtmlMatchInfo info = new HtmlMatchInfo();
            info.init(m);
            info.uploads = HtmlNodeHelper.findUpNodes(body, info.startPos, 5);

            if (!matchList.isEmpty()) {
                if (!matchList.get(0).hasSameUpNode(info)) {
                    if (matchList.size() > 1) {
                        boolean hasDirector = false;
                        for (HtmlMatchInfo item : matchList) {
                            if (item.group0.indexOf("导演") != -1) {
                                hasDirector = true;
                                break;
                            }
                        }
                        if (hasDirector) return HtmlNodeHelper.nodeString(body, matchList.get(0).matchedUpNodePos);
                        matchList.clear();
                    }
                    matchList.clear();
                }
            }
            matchList.add(info);
        }

        if (matchList.size() > 1) {
            return HtmlNodeHelper.nodeString(body, matchList.get(0).matchedUpNodePos);
        }
        return "";
    }

    @Override
    public String detailContent(List<String> ids) {
        try {
            fetchRule();
            JSONObject vinfo = new JSONObject(new String(Base64.decode(ids.get(0), BASE64_FLAG), "UTF-8"));
            JSONObject detail = rule.optJSONObject("detail");
            if (detail == null) return "";

            String vid = vinfo.optString("vod_id", "");
            String detailUrl = buildDetailUrl(detail, vid);

            String body = fetchUrl(detailUrl, detail.optJSONObject("header"));
            String content = RuleUtils.getRegion(body, detail);

            String detailTwice = getRuleVal("detail_twice");
            if (!detailTwice.isEmpty()) {
                content = applySecondCut(content, applyOrSelector(detailTwice));
            }

            if (isCssModeEnabled(detail)) {
                SpiderDebug.log("详情页使用 CSS/Jsoup 模式提取");
                JSONObject cssVod = extractDetailByCss(body, detail);
                if (cssVod.length() > 0) {
                    supplementDetailFieldsFromContext(cssVod, vinfo, vid, detailUrl);
                    playlistContent(ids, cssVod, body);
                    return buildDetailResult(cssVod);
                }
                SpiderDebug.log("CSS 提取无结果，回退到传统模式");
            }

            DetailExtractionContext ctx = locateDetailRegion(content, body);
            JSONObject vod = extractDetailFields(ctx, vinfo, vid, detailUrl);

            playlistContent(ids, vod, body);

            return buildDetailResult(vod);
        } catch (Exception e) {
            SpiderDebug.log(e);
            return "";
        }
    }

    private static class DetailExtractionContext {
        String content;
        String nodeString;
        int startPos;
        JSONArray lookback;
    }

    private String buildDetailUrl(JSONObject detail, String vid) throws JSONException {
        
        String flatUrl = getRuleVal("detail_url");
        if (!flatUrl.isEmpty()) {
            return addHttpPrefix(flatUrl.replace("${vid}", vid).replace("{vid}", vid));
        }
        if (detail.has("url")) {
            return detail.getString("url").replace("${vid}", vid).replace("{vid}", vid);
        } else if (vid.startsWith("http://") || vid.startsWith("https://") || vid.startsWith("/")) {
            return addHttpPrefix(vid);
        } else {
            JSONObject list = rule.optJSONObject("list");
            if (list != null) {
                JSONArray tmp = list.optJSONArray("vod_id");
                if (tmp != null && tmp.length() >= 2) {
                    return addHttpPrefix(tmp.getString(0) + vid + tmp.getString(1));
                }
            }
            return addHttpPrefix(vid);
        }
    }

    private DetailExtractionContext locateDetailRegion(String content, String body) throws JSONException {
        DetailExtractionContext ctx = new DetailExtractionContext();
        ctx.content = content;
        ctx.startPos = 0;

        JSONObject detail = rule.optJSONObject("detail");
        ctx.lookback = RuleUtils.getLookbackArray(detail);
        if (ctx.lookback != null) {
            int pos = content.indexOf(ctx.lookback.getString(0), 0);
            if (pos != -1) {
                List<Integer> arr = HtmlNodeHelper.findUpNodes(content, pos - 1, ctx.lookback.getInt(4));
                if (!arr.isEmpty()) {
                    ctx.startPos = arr.get(arr.size() - 1);
                    ctx.nodeString = HtmlNodeHelper.nodeString(content, ctx.startPos);
                }
            }
        }

        if (ctx.nodeString == null || ctx.nodeString.isEmpty()) {
            ctx.nodeString = guessDetailRegionUniversal(body);
        }
        if (ctx.nodeString == null || ctx.nodeString.isEmpty()) {
            ctx.nodeString = guessDetailContentRegion(body);
        }

        if (ctx.nodeString != null && !ctx.nodeString.isEmpty() && ctx.nodeString.length() != content.length()) {
            ctx.content = ctx.nodeString;
            ctx.startPos = 0;
        }
        return ctx;
    }

    private JSONObject extractDetailFields(DetailExtractionContext ctx, JSONObject vinfo,
                                             String vid, String detailUrl) throws JSONException {
        JSONObject detail = rule.optJSONObject("detail");
        JSONObject vod = new JSONObject();
        vod.put("vod_id", vinfo.optString("vod_id", ""));

        vod.put("vod_name", extractWithFallback(ctx, detail, "vod_name", vinfo, "vod_name"));
        vod.put("vod_pic", addHttpPrefix(extractWithFallback(ctx, detail, "vod_pic", vinfo, "vod_pic")));
        vod.put("type_name", RuleUtils.findSubString(ctx.content, ctx.startPos, detail.optJSONArray("type_name")));
        vod.put("vod_year", RuleUtils.findSubString(ctx.content, ctx.startPos, detail.optJSONArray("vod_year")));
        vod.put("vod_area", RuleUtils.findSubString(ctx.content, ctx.startPos, detail.optJSONArray("vod_area")));
        vod.put("vod_remarks", RuleUtils.findSubString(ctx.content, ctx.startPos, detail.optJSONArray("vod_remarks")));
        vod.put("vod_actor", RuleUtils.findSubString(ctx.content, ctx.startPos, detail.optJSONArray("vod_actor")));
        vod.put("vod_director", RuleUtils.findSubString(ctx.content, ctx.startPos, detail.optJSONArray("vod_director")));
        vod.put("vod_content", RuleUtils.findSubString(ctx.content, ctx.startPos, detail.optJSONArray("vod_content")));

        if (vod.getString("vod_name").isEmpty()) {
            vod.put("vod_name", guessValueVodName(ctx.content, ctx.startPos));
        }

        if (vod.getString("vod_pic").isEmpty()) {
            vod.put("vod_pic", guessValueVodPic(ctx.content, ctx.startPos));
        }
        
        if (vod.getString("vod_pic").isEmpty() && !playImage.isEmpty()) {
            vod.put("vod_pic", playImage);
        }
        if ("1".equals(getRuleVal("PicNeedProxy")) && !vod.getString("vod_pic").isEmpty()) {
            vod.put("vod_pic", fixCover(vod.getString("vod_pic"), detailUrl));
        }

        supplementDetailFields(vod, ctx);

        applyDetailSeparator(vod, ctx);
        
        mergeDetailFields(vod);

        return vod;
    }

    private void applyDetailSeparator(JSONObject vod, DetailExtractionContext ctx) throws JSONException {
        String sep = getRuleVal("detail_separator");
        if (sep.isEmpty()) return;
        
        String text = HtmlNodeHelper.trimHtmlString(ctx.content, "!!!!");
        String[] labels = sep.split("\\|");
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i].trim();
            if (label.isEmpty()) continue;
            int at = text.indexOf(label);
            if (at < 0) continue;
            int from = at + label.length();
            int to = text.length();
            for (int j = i + 1; j < labels.length; j++) {
                String next = labels[j].trim();
                if (next.isEmpty()) continue;
                int p = text.indexOf(next, from);
                if (p >= 0 && p < to) to = p;
            }
            String value = text.substring(from, to).trim();
            value = value.replaceFirst("^[：:]\s*", "").replaceAll("!{2,}", " ").trim();
            String field = mapDetailLabel(label);
            if (field != null && !value.isEmpty() && value.length() < 500
                    && vod.optString(field, "").isEmpty()) {
                vod.put(field, value);
            }
        }
    }

    private static String mapDetailLabel(String label) {
        if (label.contains("导演")) return "vod_director";
        if (label.contains("主演") || label.contains("演员")) return "vod_actor";
        if (label.contains("地区") || label.contains("国家")) return "vod_area";
        if (label.contains("年份") || label.contains("年代")) return "vod_year";
        if (label.contains("状态") || label.contains("备注") || label.contains("更新")) return "vod_remarks";
        if (label.contains("类型") || label.contains("分类")) return "type_name";
        if (label.contains("简介") || label.contains("剧情") || label.contains("介绍")) return "vod_content";
        return null;
    }

    private void mergeDetailFields(JSONObject vod) throws JSONException {
        String merge = getRuleVal("detail_merge");
        if (merge.isEmpty()) return;
        StringBuilder sb = new StringBuilder(vod.optString("vod_content", ""));
        for (String raw : merge.split("[,，]")) {
            String f = raw.trim();
            if (f.isEmpty()) continue;
            String field = mapDetailLabel(f);
            if (field == null) field = XBPQKey.norm(f);
            String val = vod.optString(field, "");
            if (val.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            // mapDetailLabel 返回内部 vod_* 键，不在 XBPQKey 中文表里；直接用配置词作显示名
            sb.append(f).append("：").append(val);
        }
        vod.put("vod_content", sb.toString());
    }

    private String extractWithFallback(DetailExtractionContext ctx, JSONObject detail,
                                        String field, JSONObject source, String sourceField) throws JSONException {
        String value = RuleUtils.findSubString(ctx.content, ctx.startPos, detail.optJSONArray(field));
        if (value.isEmpty()) {
            value = source.optString(sourceField, "");
        }
        return value;
    }

    private void supplementDetailFields(JSONObject vod, DetailExtractionContext ctx) throws JSONException {
        if (ctx.lookback == null || ctx.lookback.length() <= 1) {
            
            guessSupplementDetailFields(vod, ctx);
        } else {
            
            lookbackSupplementDetailFields(vod, ctx);
        }
    }

    private void lookbackSupplementDetailFields(JSONObject vod, DetailExtractionContext ctx) throws JSONException {
        JSONArray key = new JSONArray();
        String name = ctx.lookback.getString(0);
        String skey = findSimilarKeyName(name);

        key.put(name);
        key.put(ctx.lookback.getString(1));

        if (vod.getString("vod_director").isEmpty()) {
            key.put(0, name.replace(skey, "导演"));
            vod.put("vod_director", RuleUtils.findSubString(ctx.content, ctx.startPos, key));
        }
        if (vod.getString("vod_actor").isEmpty()) {
            key.put(0, name.replace(skey, "主演"));
            vod.put("vod_actor", RuleUtils.findSubString(ctx.content, ctx.startPos, key));
        }
        if (vod.getString("vod_content").isEmpty()) {
            vod.put("vod_content", extractLongestText(ctx.content));
        }
    }

    private String findSimilarKeyName(String name) {
        List<String> candidates = Arrays.asList("导演", "演员", "类型", "年份");
        for (String candidate : candidates) {
            if (name.indexOf(candidate) != -1) return candidate;
        }
        return name;
    }

    private String extractLongestText(String content) {
        String all = HtmlNodeHelper.trimHtmlString(content, "!!!!");
        String[] words = all.split("!!!!");
        String longest = "";
        for (String word : words) {
            if (word.length() > longest.length()) longest = word;
        }
        return HtmlNodeHelper.trimHtmlString(longest);
    }

    private void guessSupplementDetailFields(JSONObject vod, DetailExtractionContext ctx) throws JSONException {
        if (ctx.nodeString == null || ctx.nodeString.isEmpty()) return;

        List<String> childNodes = HtmlNodeHelper.getChildNodes(ctx.nodeString);
        String content = "";
        String delimiter = TextUtils.join("|", detailFieldNames);
        Pattern pattern = Pattern.compile(delimiter, Pattern.CASE_INSENSITIVE);

        for (String node : childNodes) {
            String text = HtmlNodeHelper.trimHtmlString(node, " ").replace("：", "");
            if (text.length() > content.length()) content = text;

            String[] items = text.split(delimiter);
            List<String> nonEmptyItems = new ArrayList<>();
            for (String item : items) {
                if (!item.isEmpty()) nonEmptyItems.add(item);
            }

            Matcher m = pattern.matcher(text);
            int index = 0;
            while (m.find() && index < nonEmptyItems.size()) {
                String matched = m.group(0);
                for (int j = 0; j < detailFieldNames.size(); ++j) {
                    if (matched.indexOf(detailFieldNames.get(j)) != -1) {
                        String key = detailFieldKeys.get(j);
                        if (vod.getString(key).isEmpty()) {
                            vod.put(key, nonEmptyItems.get(index).trim());
                        }
                        break;
                    }
                }
                ++index;
            }
        }

        if (vod.getString("vod_content").isEmpty()) {
            vod.put("vod_content", content);
        }
    }

    private String buildDetailResult(JSONObject vod) throws JSONException {
        JSONArray list = new JSONArray();
        list.put(vod);
        JSONObject result = new JSONObject();
        
        result.put("code", CODE_OK);
        result.put("msg", "ok");
        result.put("data", vod);
        result.put("list", list);
        return result.toString();
    }

    protected void playlistContent(List<String> ids, JSONObject vod, String body) {
        try {
            fetchRule();
            JSONObject vinfo = new JSONObject(new String(Base64.decode(ids.get(0), BASE64_FLAG), "UTF-8"));

            JSONObject playlist = rule.optJSONObject("playlist");
            if (playlist == null) return;

            if (playlist.has("url")) {
                JSONObject detailRule = rule.optJSONObject("detail");
                String detailUrl = (detailRule == null) ? "" : detailRule.optString("url", "");
                String playListUrl = playlist.getString("url");
                if (!detailUrl.equals(playListUrl)) {
                    String url = playListUrl.replace("{vid}", vinfo.getString("vod_id"));
                    body = fetchUrl(url, playlist.optJSONObject("header"));
                }
            }

            String content = RuleUtils.getRegion(body, playlist);

            String playTwice = getRuleVal("play_twice");
            if (!playTwice.isEmpty()) {
                content = applySecondCut(content, applyOrSelector(playTwice));
            }

            List<String> vodPlayUrl = obtainPlayUrlList(content, playlist, vinfo);
            List<String> vodPlayFrom = findVodPlayFrom(content, vodPlayUrl == null ? 0 : vodPlayUrl.size(), body);
            alignPlaySources(vodPlayFrom, vodPlayUrl);

            String epiPrefix = resolveEpiUrlVal(getRuleVal("epiurl_prefix"), body, currentDetailUrl(vinfo));
            String epiSuffix = resolveEpiUrlVal(getRuleVal("epiurl_suffix"), body, currentDetailUrl(vinfo));
            if (!epiPrefix.isEmpty() || !epiSuffix.isEmpty()) {
                vodPlayUrl = applyEpiUrlAdjust(vodPlayUrl, epiPrefix, epiSuffix);
            }

            vodPlayUrl = processEpisodes(vodPlayUrl);

            applyLineNameMap(vodPlayFrom);

            boolean noEpisodes = vodPlayUrl == null || vodPlayUrl.isEmpty()
                    || (vodPlayUrl.size() == 1 && vodPlayUrl.get(0).trim().isEmpty());
            String emptyPlayUrl = getRuleVal("empty_play_url");
            if (noEpisodes && !emptyPlayUrl.isEmpty()) {
                String lineName = getRuleVal("empty_play_from");
                if (lineName.isEmpty()) lineName = "空播放";
                vodPlayUrl = new ArrayList<>(1);
                vodPlayUrl.add(emptyPlayUrl.contains("$") ? emptyPlayUrl : "第1集$" + emptyPlayUrl);
                vodPlayFrom = new ArrayList<>(1);
                vodPlayFrom.add(lineName);
                SpiderDebug.log("空播放兜底：已补占位线路 " + lineName);
            }

            reorderPlaySources(playlist, vodPlayUrl, vodPlayFrom);

            if (mergeLines && vodPlayUrl.size() > 1) {
                String firstName = !vodPlayFrom.isEmpty() ? vodPlayFrom.get(0) : "线路";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < vodPlayUrl.size(); i++) {
                    if (i > 0) sb.append("#");
                    sb.append(vodPlayUrl.get(i));
                }
                vodPlayUrl.clear();
                vodPlayUrl.add(sb.toString());
                vodPlayFrom.clear();
                vodPlayFrom.add(firstName);
                SpiderDebug.log("线路合并：已将 " + vodPlayUrl.size() + " 条线路合并为单一线路");
            }

            vod.put("vod_play_url", TextUtils.join("$$$", vodPlayUrl));
            vod.put("vod_play_from", TextUtils.join("$$$", vodPlayFrom));
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    private List<String> processEpisodes(List<String> vodPlayUrl) {
        if (vodPlayUrl == null || vodPlayUrl.isEmpty()) return vodPlayUrl;
        boolean dedup = "1".equals(getRuleVal("episode_dedup"));
        boolean sort = "1".equals(getRuleVal("episode_sort"));
        List<String> qualityPriority = parseQualityPriority();
        if (!dedup && !sort && qualityPriority.isEmpty()) return vodPlayUrl;
        try {
            List<String> result = new ArrayList<>(vodPlayUrl.size());
            for (String line : vodPlayUrl) {
                String[] eps = line.split("#");
                List<String[]> kept = new ArrayList<>(eps.length);
                Set<String> seenUrl = new HashSet<>();
                for (String ep : eps) {
                    int idx = ep.indexOf('$');
                    String name = idx >= 0 ? ep.substring(0, idx) : ep;
                    String url = idx >= 0 ? ep.substring(idx + 1) : "";
                    if (url.isEmpty()) continue;
                    if (dedup && !seenUrl.add(url)) continue;
                    kept.add(new String[]{name, url});
                }
                
                if (!qualityPriority.isEmpty() && kept.size() > 1) {
                    kept = pickBestQuality(kept, qualityPriority);
                }
                if (sort && kept.size() > 1) {
                    boolean allNumeric = true;
                    for (String[] p : kept) {
                        if (extractEpisodeNum(p[0]) == Integer.MAX_VALUE) {
                            allNumeric = false;
                            break;
                        }
                    }
                    if (allNumeric) kept.sort(Comparator.comparingInt(p -> extractEpisodeNum(p[0])));
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < kept.size(); i++) {
                    if (i > 0) sb.append('#');
                    sb.append(kept.get(i)[0]).append('$').append(kept.get(i)[1]);
                }
                result.add(sb.toString());
            }
            SpiderDebug.log("剧集清洗完成: 去重=" + dedup + " 排序=" + sort
                    + (qualityPriority.isEmpty() ? "" : " 清晰度优先=" + qualityPriority));
            return result;
        } catch (Exception e) {
            SpiderDebug.log("processEpisodes error: " + e.getMessage());
            return vodPlayUrl;
        }
    }

    private List<String> parseQualityPriority() {
        String cfg = getRuleVal("episode_quality");
        List<String> list = new ArrayList<>();
        if (cfg.isEmpty()) return list;
        for (String s : cfg.split("[,，、]")) {
            String t = s.trim().toLowerCase();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    private List<String[]> pickBestQuality(List<String[]> kept, List<String> priority) {
        LinkedHashMap<String, String[]> best = new LinkedHashMap<>(kept.size() * 2);
        for (String[] p : kept) {
            int num = extractEpisodeNum(p[0]);
            String key = num != Integer.MAX_VALUE ? "n:" + num : "s:" + p[0];
            String[] cur = best.get(key);
            if (cur == null || qualityRank(p, priority) < qualityRank(cur, priority)) {
                best.put(key, p);
            }
        }
        return new ArrayList<>(best.values());
    }

    private static int qualityRank(String[] ep, List<String> priority) {
        String hay = (ep[0] + " " + ep[1]).toLowerCase();
        for (int i = 0; i < priority.size(); i++) {
            if (hay.contains(priority.get(i))) return i;
        }
        return Integer.MAX_VALUE;
    }

    private static int extractEpisodeNum(String epText) {
        try {
            Matcher m = P_EPISODE_NUM.matcher(epText == null ? "" : epText);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {
        }
        return Integer.MAX_VALUE;
    }

    private void applyLineNameMap(List<String> vodPlayFrom) {
        if (vodPlayFrom == null || vodPlayFrom.isEmpty()) return;
        String cfg = getRuleVal("line_name_map");
        if (cfg.isEmpty()) return;
        try {
            Map<String, String> map = new HashMap<>();
            for (String pair : cfg.split("#")) {
                int idx = pair.indexOf(':');
                if (idx > 0) map.put(pair.substring(0, idx).trim(), pair.substring(idx + 1).trim());
            }
            for (int i = 0; i < vodPlayFrom.size(); i++) {
                String renamed = map.get(vodPlayFrom.get(i).trim());
                if (renamed != null && !renamed.isEmpty()) vodPlayFrom.set(i, renamed);
            }
        } catch (Exception e) {
            SpiderDebug.log("applyLineNameMap error: " + e.getMessage());
        }
    }

    private List<String> obtainPlayUrlList(String content, JSONObject playlist, JSONObject vinfo) throws JSONException {
        List<String> vodPlayUrl = null;
        if (!playlist.has("vod_play_url")) {
            JSONArray guessedRule = guessRuleVodPlayUrl(content, vinfo.getString("vod_id"));
            if (guessedRule != null) {
                playlist.put("vod_play_url", guessedRule);
            }
        }
        vodPlayUrl = findVodPlayUrl(content);

        if (vodPlayUrl == null || vodPlayUrl.isEmpty()) {
            String breadcrumbLink = tryExtractFromBreadcrumb(content);
            if (breadcrumbLink != null) {
                SpiderDebug.log("面包屑兜底：" + breadcrumbLink);
                vodPlayUrl = new ArrayList<>(1);
                vodPlayUrl.add(breadcrumbLink);
            }
        }
        return vodPlayUrl;
    }

    private String currentDetailUrl(JSONObject vinfo) {
        try {
            return buildDetailUrl(rule.getJSONObject("detail"), vinfo.optString("vod_id", ""));
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveEpiUrlVal(String cfg, String html, String pageUrl) {
        if (cfg.isEmpty()) return "";
        String val = applyPostProcessors(applyOrSelector(cfg));
        if (val.contains("&&")) {
            val = extractField(html, val);
        }
        if (val.contains("PG_URL")) {
            val = val.replace("PG_URL", pageUrl).replaceAll("'", "");
        }
        return val.trim();
    }

    private List<String> applyEpiUrlAdjust(List<String> lines, String prefix, String suffix) {
        if (lines == null || lines.isEmpty()) return lines;
        List<String> adjusted = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                adjusted.add(line);
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (String seg : line.split("#")) {
                int d = seg.indexOf('$');
                String title = d >= 0 ? seg.substring(0, d) : seg;
                String url = d >= 0 ? seg.substring(d + 1) : "";
                if (url.isEmpty()) continue;
                String newUrl = prefix.isEmpty() ? addHttpPrefix(url) : prefix + url;
                if (!suffix.isEmpty()) newUrl = newUrl + suffix;
                if (sb.length() > 0) sb.append('#');
                sb.append(title).append('$').append(newUrl);
            }
            adjusted.add(sb.toString());
        }
        return adjusted;
    }

    private String tryExtractFromBreadcrumb(String content) {
        if (content == null) return null;
        
        String targetHref = extractRelativeVideoHref(content);
        if (targetHref == null) return null;

        String region = extractBreadcrumbRegion(content);
        if (region != null && !region.isEmpty()) {
            String link = extractHrefFromRegion(region, targetHref);
            if (link != null) {
                return makeTitleDollarLink(region, link);
            }
        }
        
        return makeTitleDollarLink(content, targetHref);
    }

    private String extractRelativeVideoHref(String content) {
        
        String region = extractBreadcrumbRegion(content);
        if (region != null) {
            String m = findFirstRelativeShipinHref(region);
            if (m != null) return m;
        }
        
        return findFirstRelativeShipinHref(content);
    }

    private static final String[] BREADCRUMB_PATTERNS = {
            "<div class=\"pc\">&&</div>",
            "<div class=\"pc crumbs\">&&</div>",
            "<div class=\"crumbs\">&&</div>",
            "<ul class=\"nav-bread\">&&</ul>",
            "<ul class=\"nav_bread\">&&</ul>",
    };

    private String extractBreadcrumbRegion(String content) {
        for (String pattern : BREADCRUMB_PATTERNS) {
            
            JSONArray keys = stringCutToLookback(pattern);
            if (keys != null) {
                String region = RuleUtils.findSubString(content, 0, keys);
                if (!region.isEmpty()) return region;
            }
        }
        
        int bi = content.indexOf("class=\"bread\"");
        if (bi >= 0) {
            int li = content.lastIndexOf('<', bi);
            if (li >= 0) {
                int ri = content.indexOf('>', bi);
                if (ri > li) {
                    int end = content.indexOf("</", ri);
                    if (end > ri) {
                        return content.substring(li, end + 2);
                    }
                }
            }
        }
        return null;
    }

    private String findFirstRelativeShipinHref(String text) {
        if (text == null) return null;
        
        String prefix = "href=\"/shipin/";
        int idx = text.indexOf(prefix);
        if (idx < 0) {
            
            prefix = "href=\"/play/";
            idx = text.indexOf(prefix);
        }
        if (idx < 0) return null;
        int hrefStart = idx + prefix.length();
        int hrefEnd = text.indexOf('"', hrefStart);
        return hrefEnd > hrefStart ? text.substring(hrefStart, hrefEnd) : null;
    }

    private String extractHrefFromRegion(String region, String targetHref) {
        if (region == null || targetHref == null) return null;
        int ai = region.indexOf(targetHref);
        if (ai < 0) return null;
        int li = Math.max(0, region.lastIndexOf('<', ai));
        int ri = region.indexOf('>', ai);
        if (li < 0 || ri < 0 || ri <= li) return null;
        String anchor = region.substring(li, ri + 1);
        int hi = anchor.indexOf("href=\"");
        if (hi < 0) return null;
        int hs = hi + 6;
        int he = anchor.indexOf('"', hs);
        if (he <= hs) return null;
        return anchor.substring(hs, he);
    }

    private String makeTitleDollarLink(String region, String href) {
        if (region == null || href == null) return null;
        int li = region.lastIndexOf('<', region.indexOf(href));
        if (li < 0) li = 0;
        int ri = region.indexOf('>', li);
        if (ri <= li) return null;
        String anchor = region.substring(li, ri + 1);
        
        int ti = anchor.indexOf('>', li) + 1;
        int te = anchor.indexOf('<', ti);
        String title = (te > ti) ? anchor.substring(ti, te).trim() : "";
        if (title.isEmpty()) title = "第1集";
        
        String absoluteUrl = addHttpPrefix(href);
        return title + "$" + absoluteUrl;
    }

    private void reorderPlaySources(JSONObject playlist, List<String> vodPlayUrl,
                                      List<String> vodPlayFrom) throws JSONException {
        if (!playlist.has("vod_play_from") || vodPlayUrl == null || vodPlayUrl.isEmpty()) return;

        String joinedFrom = TextUtils.join("$$$", vodPlayFrom);
        String defaultFrom = TextUtils.join("$$$", makeVodPlayFrom(vodPlayUrl.size()));
        if (joinedFrom.equals(defaultFrom)) return;

        List<String> urls = new ArrayList<>();
        List<String> froms = new ArrayList<>();
        Set<Integer> matched = new HashSet<>();

        JSONArray rulePlayFrom = playlist.getJSONArray("vod_play_from");
        for (int i = 0; i < rulePlayFrom.length(); ++i) {
            Object entry = rulePlayFrom.get(i);
            String alias = "";
            if (entry instanceof String) {
                alias = (String) entry;
            } else if (entry instanceof JSONArray) {
                JSONArray item = (JSONArray) entry;
                alias = item.getString(0);
                if (item.length() > 1) alias = item.getString(1);
            }

            for (int j = 0; j < vodPlayFrom.size(); ++j) {
                if (matched.contains(j)) continue;
                if (vodPlayFrom.get(j).equals(alias)) {
                    urls.add(vodPlayUrl.get(j));
                    froms.add(vodPlayFrom.get(j));
                    matched.add(j);
                }
            }
        }

        for (int j = 0; j < vodPlayFrom.size(); ++j) {
            if (matched.contains(j)) continue;
            urls.add(vodPlayUrl.get(j));
            froms.add(vodPlayFrom.get(j));
        }

        if (!urls.isEmpty()) {
            vodPlayUrl.clear();
            vodPlayUrl.addAll(urls);
            vodPlayFrom.clear();
            vodPlayFrom.addAll(froms);
        }
    }

    protected String parsePlayUrl(String flag, String url, String html, List<String> list) {
        try {
            JSONObject play = rule.optJSONObject("play");
            if (play == null) return "";

            String body = RuleUtils.getRegion(html, play);
            int startPos = 0;

            JSONArray lookback = RuleUtils.getLookbackArray(play);
            if (lookback != null) {
                int pos = body.indexOf(lookback.getString(0), 0);
                if (pos != -1) {
                    List<Integer> arr = HtmlNodeHelper.findUpNodes(body, pos - 1, lookback.getInt(4));
                    if (!arr.isEmpty()) {
                        startPos = arr.get(arr.size() - 1);
                    } else {
                        startPos = pos;
                    }
                }
            }

            String vodUrl = RuleUtils.findSubString(body, startPos, play.optJSONArray("vod_url"));
            vodUrl = vodUrl.replace("\\/", "/");
            
            vodUrl = hexEscapeDecode(vodUrl);
            
            vodUrl = tryDecryptParsedUrl(body, vodUrl);
            if (vodUrl.isEmpty()) return "";
            if (!isDirectLinkUniversal(vodUrl) && !isVideoFormat(vodUrl)) return "";

            JSONObject result = new JSONObject();
            result.put("parse", 0);
            result.put("playUrl", "");
            result.put("url", vodUrl);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }


    /** 当前 playerContent 调用传入的 VIP 域名列表（框架透传，此前从未被消费） */
    private List<String> currentVipFlags = null;

    /** VIP 判定：内置域名表 + 框架透传的 vipFlags 域名列表 */
    private boolean isVipPlayUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        if (Util.isVip(url)) return true;
        if (currentVipFlags != null) {
            String lower = url.toLowerCase();
            for (String vf : currentVipFlags) {
                if (vf != null && !vf.trim().isEmpty() && lower.contains(vf.trim().toLowerCase())) return true;
            }
        }
        return false;
    }

    @Override
    public String playerContent(String flag, String url, List<String> vipFlags) throws Exception {
        try {
            fetchRule();
            initEnhancedConfig();
            this.currentVipFlags = vipFlags;

            String forcePlayResult = tryForcePlay(url);
            if (forcePlayResult != null) return appendDanmuParam(forcePlayResult);

            JSONObject play = rule.optJSONObject("play");
            String html = fetchUrl(url, play == null ? null : play.optJSONObject("header"));

            String macPlayerResult = tryMacPlayer(html);
            if (macPlayerResult != null) return appendDanmuParam(macPlayerResult);

            String directResult = parsePlayUrl(flag, url, html, vipFlags);
            if (!directResult.isEmpty()) return appendDanmuParam(directResult);

            String jumpResult = tryJumpUrl(url, html);
            if (jumpResult != null) return appendDanmuParam(jumpResult);

            // 兜底：规则提取失败时，直接扫描播放页中的视频直链
            String scannedResult = tryScanVideoLink(url, html);
            if (scannedResult != null) return appendDanmuParam(scannedResult);

            return buildSniffResult(url);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private String appendDanmuParam(String playerResult) {
        if (playerResult == null || playerResult.isEmpty()) return playerResult;
        try {
            JSONObject result = new JSONObject(playerResult);

            String playUrl = result.optString("url", "");
            if (!playUrl.isEmpty() && result.optInt("parse", 1) == 0) {
                result.put("url", stripPlayUrlParams(playUrl));
            }

            String danmuUrl = getRuleVal("danmuUrl");
            if (!danmuUrl.isEmpty()) {
                result.put("danmaku", "proxy://do=XBPQ&danmu_url="
                        + URLEncoder.encode(danmuUrl, "UTF-8"));
            }
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("appendDanmuParam error: " + e.getMessage());
            return playerResult;
        }
    }

    private String stripPlayUrlParams(String playUrl) {
        String cfg = getRuleVal("play_strip_params");
        if (cfg.isEmpty()) return playUrl;
        try {
            Set<String> drop = new HashSet<>();
            for (String p : cfg.split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) drop.add(t);
            }
            if (drop.isEmpty()) return playUrl;
            int hash = playUrl.indexOf('#');
            String frag = hash >= 0 ? playUrl.substring(hash) : "";
            String main = hash >= 0 ? playUrl.substring(0, hash) : playUrl;
            int q = main.indexOf('?');
            if (q < 0) return playUrl;
            String[] pairs = main.substring(q + 1).split("&");
            StringBuilder kept = new StringBuilder();
            for (String pair : pairs) {
                if (pair.isEmpty()) continue;
                String key = pair.split("=", 2)[0];
                if (drop.contains(key)) continue;
                if (kept.length() > 0) kept.append('&');
                kept.append(pair);
            }
            String result = kept.length() == 0 ? main.substring(0, q) : main.substring(0, q + 1) + kept;
            return result + frag;
        } catch (Exception e) {
            SpiderDebug.log("stripPlayUrlParams error: " + e.getMessage());
            return playUrl;
        }
    }

    private String tryForcePlay(String url) throws JSONException {
        String forcePlay = rule.optString("force_play", "0");
        if (!"1".equals(forcePlay) && !"2".equals(forcePlay)) return null;

        JSONObject result = new JSONObject();
        String webUrl = rule.optString("play_prefix", "") + url + rule.optString("play_suffix", "");

        applyPlayHeader(result, webUrl);

        boolean forceVideo = webUrl.contains("#isVideo=true#");
        webUrl = webUrl.replaceAll("#isVideo=true#", "");

        if (Util.isVideoFormat(webUrl) || forceVideo) {
            result.put("parse", 0);
            result.put("playUrl", "");
        } else if (Util.isThunder(webUrl)) {
            
            result.put("parse", 0);
            result.put("playUrl", "");
        } else if (isVipPlayUrl(webUrl)) {
            result.put("parse", 1);
            result.put("jx", "1");
            result.put("url", webUrl);
            return result.toString();
        } else {
            result.put("parse", 1);
            result.put("playUrl", "");
        }
        result.put("url", webUrl);
        return result.toString();
    }

    private void applyPlayHeader(JSONObject result, String webUrl) throws JSONException {
        String playHeader = getRuleVal("play_header");
        if (playHeader.isEmpty()) return;

        if (playHeader.startsWith("{")) {
            result.put("header", playHeader);
        } else {
            JSONObject hdr = new JSONObject();
            for (String user : playHeader.split("#")) {
                String[] head = user.split("\\$");
                if (head.length >= 2) hdr.put(head[0], " " + head[1]);
            }
            result.put("header", hdr.toString());
        }
    }

    private String tryMacPlayer(String html) throws Exception {

        String mode = rule.optString("Anal_MacPlayer", "0");
        if (!"1".equals(mode) && !"2".equals(mode)) {
            // auto_maccms 为 Anal_MacPlayer 的别名触发键：此前从未被读取，接线只影响显式配置了该键的规则
            mode = getRuleVal("auto_maccms");
        }
        if (!"1".equals(mode) && !"2".equals(mode)) return null;

        // 内部独立容错：单个播放器对象解析异常不应中断整条播放链
        try {
            Pattern scriptPattern = P_PLAYER_OBJ;
            Matcher scriptMatcher = scriptPattern.matcher(html);
            if (!scriptMatcher.find()) return null;

            JSONObject player = new JSONObject(scriptMatcher.group(1));
            String videoUrl = player.optString("url", "");
            if (videoUrl.isEmpty()) return null;

            if (player.has("encrypt")) {
                videoUrl = decryptPlayerUrl(videoUrl, player.optInt("encrypt", 0));
            }

            return buildPlayerResult(videoUrl);
        } catch (Exception e) {
            SpiderDebug.log(safeLog("tryMacPlayer 解析失败: " + e.getMessage()));
            return null;
        }
    }

    private String decryptPlayerUrl(String url, int encrypt) throws Exception {
        String result;
        if (encrypt == 1) {
            result = java.net.URLDecoder.decode(url, "UTF-8");
        } else if (encrypt == 2) {
            String decoded = new String(Base64.decode(url, Base64.DEFAULT), "UTF-8");
            result = java.net.URLDecoder.decode(decoded, "UTF-8");
        } else {
            result = url;
        }
        return decodeEscapesDeep(result);
    }

    private String buildPlayerResult(String videoUrl) throws JSONException {
        if (isVipPlayUrl(videoUrl)) {
            JSONObject result = new JSONObject();
            result.put("parse", 1);
            result.put("jx", "1");
            result.put("url", videoUrl);
            return result.toString();
        } else if (Util.isVideoFormat(videoUrl)) {
            JSONObject result = new JSONObject();
            result.put("parse", 0);
            result.put("playUrl", "");
            result.put("url", videoUrl);
            return result.toString();
        }
        return null;
    }

    private String tryJumpUrl(String webUrl, String html) throws Exception {
        String jumpUrl = rule.optString("jump_url", "");
        if (jumpUrl.isEmpty()) return null;

        jumpUrl = applyPostProcessors(jumpUrl);
        String[] parts = jumpUrl.split("&&", 2);
        String startFlag = parts[0];
        String endFlag = parts.length > 1 ? parts[1] : "";

        String parsedUrl = extractJumpUrl(html, startFlag, endFlag);
        if (parsedUrl.isEmpty()) return null;

        parsedUrl = tryDecryptParsedUrl(html, parsedUrl);
        parsedUrl = parsedUrl.replace("\\/", "/");

        if (parsedUrl.isEmpty()) return null;

        // 相对路径/协议相对路径补全为绝对地址，避免播放器拿到不可播的地址
        if (!parsedUrl.startsWith("http://") && !parsedUrl.startsWith("https://")) {
            String resolved = parsedUrl.startsWith("//")
                    ? (webUrl != null && webUrl.startsWith("https") ? "https:" + parsedUrl : "http:" + parsedUrl)
                    : resolveRedirectTarget(parsedUrl, webUrl);
            if (!resolved.isEmpty()) parsedUrl = resolved;
        }

        // 解析后仍是相对路径则不作为直链返回，交给后续兜底策略
        String lowerParsed = parsedUrl.toLowerCase();
        if (!lowerParsed.startsWith("http://") && !lowerParsed.startsWith("https://")
                && !lowerParsed.startsWith("magnet:") && !lowerParsed.startsWith("thunder:")) {
            return null;
        }

        if (Util.isVideoFormat(parsedUrl) || isVideoFormat(parsedUrl)) {
            JSONObject result = new JSONObject();
            result.put("parse", 0);
            result.put("playUrl", "");
            result.put("url", parsedUrl);
            return result.toString();
        }
        if (isVipPlayUrl(parsedUrl)) {
            JSONObject result = new JSONObject();
            result.put("parse", 1);
            result.put("jx", "1");
            result.put("url", parsedUrl);
            return result.toString();
        }
        return null;
    }

    private String extractJumpUrl(String html, String startFlag, String endFlag) {
        String result = "";
        if (startFlag.contains("*")) {
            Matcher m = P_PLAYER_OBJ.matcher(html);
            if (m.find()) {
                try {
                    result = new JSONObject(m.group(1)).optString("url", "");
                    if (!result.isEmpty()) return decodeEscapesDeep(result);
                } catch (JSONException e) {
                    SpiderDebug.log(e);
                }
            }
            Matcher um = P_PLAYER_URL.matcher(html);
            if (um.find()) return decodeEscapesDeep(um.group(1));
        } else {
            List<String> results = subContent(html, startFlag, endFlag);
            if (!results.isEmpty()) return decodeEscapesDeep(results.get(0));
        }
        return "";
    }

    private String tryDecryptParsedUrl(String html, String parsedUrl) {
        try {
            Pattern ep = P_ENCRYPT;
            Matcher em = ep.matcher(html);
            if (em.find()) {
                int encrypt = Integer.parseInt(em.group(1));
                return decryptPlayerUrl(parsedUrl, encrypt);
            }
        } catch (Exception e) {
            SpiderDebug.log("解密播放地址失败，返回原始URL: " + e.getMessage());
        }
        return parsedUrl;
    }

    /**
     * 播放页兜底提取：规则 jump_url 提取失败时，直接从播放页 HTML 中提取视频直链。
     * 策略链：MacCMS 播放器对象(var player_xxx={...}) → 通用 m3u8/mp4 直链扫描。
     */
    private String tryScanVideoLink(String webUrl, String html) throws Exception {
        if (html == null || html.isEmpty()) return null;

        // 策略1：MacCMS 标准播放器对象（不依赖 Anal_MacPlayer 配置）
        try {
            Matcher m = P_PLAYER_OBJ.matcher(html);
            if (m.find()) {
                JSONObject player = new JSONObject(m.group(1));
                String videoUrl = player.optString("url", "");
                if (!videoUrl.isEmpty()) {
                    if (player.has("encrypt")) {
                        videoUrl = decryptPlayerUrl(videoUrl, player.getInt("encrypt"));
                    } else {
                        videoUrl = decodeEscapesDeep(videoUrl);
                    }
                    if (!videoUrl.startsWith("http") && !videoUrl.startsWith("//")) {
                        videoUrl = resolveRedirectTarget(videoUrl, webUrl);
                    }
                    // 解析后仍是相对路径则放弃该结果，交给策略2/嗅探
                    String lowerVideo = videoUrl.toLowerCase();
                    if (!lowerVideo.startsWith("http://") && !lowerVideo.startsWith("https://")) {
                        videoUrl = "";
                    }
                    if (!videoUrl.isEmpty()) {
                        String built = buildPlayerResult(videoUrl);
                        if (built != null) return built;
                    }
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(safeLog("tryScanVideoLink 播放器对象提取失败: " + e.getMessage()));
        }

        // 策略2：通用 m3u8/mp4 直链扫描
        try {
            Matcher vm = P_VIDEO_DIRECT.matcher(html);
            while (vm.find()) {
                String candidate = decodeEscapesDeep(vm.group());
                if (candidate.startsWith("//")) {
                    candidate = (webUrl != null && webUrl.startsWith("https") ? "https:" : "http:") + candidate;
                }
                if (Util.isVideoFormat(candidate) || isVideoFormat(candidate)) {
                    SpiderDebug.log(safeLog("播放页直链兜底命中: " + candidate));
                    JSONObject result = new JSONObject();
                    result.put("parse", 0);
                    result.put("playUrl", "");
                    result.put("url", candidate);
                    return result.toString();
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(safeLog("tryScanVideoLink 直链扫描失败: " + e.getMessage()));
        }
        return null;
    }

    private String buildSniffResult(String url) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("parse", 1);
        result.put("playUrl", "");
        result.put("url", url);
        return result.toString();
    }

    protected Object parseJsonSearchResult(Object obj) {
        try {
            if (obj == null) return null;
            JSONObject search = rule.optJSONObject("search");
            if (search == null) return null;

            String keyVodId = search.getString("vod_id");
            String keyVodName = search.getString("vod_name");

            if (obj instanceof JSONObject) {
                JSONObject object = (JSONObject) obj;
                if (object.has(keyVodId) && object.has(keyVodName)) return object;
                for (Iterator<String> iter = object.keys(); iter.hasNext();) {
                    Object r = parseJsonSearchResult(object.get(iter.next()));
                    if (r != null) return r;
                }
            } else if (obj instanceof JSONArray) {
                JSONArray array = (JSONArray) obj;
                for (int i = 0; i < array.length(); ++i) {
                    if (parseJsonSearchResult(array.get(i)) != null) return array;
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    protected String parseSearchResult(String body, String page) {
        try {
            JSONObject obj = new JSONObject(body);
            Object info = parseJsonSearchResult(obj);
            if (info == null) return "";

            JSONArray arr;
            if (info instanceof JSONObject) {
                arr = new JSONArray();
                arr.put((JSONObject) info);
            } else {
                arr = (JSONArray) info;
            }

            return buildSearchResults(arr, page);
        } catch (Exception e) {
            
        }
        return "";
    }

    private String buildSearchResults(JSONArray arr, String page) throws JSONException {
        JSONObject search = rule.optJSONObject("search");
        if (search == null) return "";
        // JSON 模式要求 vod_id/vod_name 等取值为「字段名」字符串；配置成截取数组时说明不是 JSON 数据
        String idKey = optJsonFieldName(search, "vod_id");
        if (idKey == null) return "";
        String nameKey = optJsonFieldName(search, "vod_name");
        String picKey = optJsonFieldName(search, "vod_pic");
        String remarksKey = optJsonFieldName(search, "vod_remarks");
        JSONArray videos = new JSONArray();

        for (int i = 0; i < arr.length(); ++i) {
            JSONObject o = arr.getJSONObject(i);
            if (!o.has(idKey)) continue;

            JSONObject v = new JSONObject();
            v.put("vod_id", o.get(idKey).toString());
            v.put("vod_name", nameKey != null && o.has(nameKey) ? o.get(nameKey).toString() : "未知");
            v.put("vod_pic", picKey != null && o.has(picKey) ? o.get(picKey).toString() : "");
            v.put("vod_remarks", remarksKey != null && o.has(remarksKey) ? o.get(remarksKey).toString() : "");
            v.put("vod_id", encodeVodId(v));
            videos.put(v);
        }

        return wrapList(videos, null, page).toString();
    }

    /** 取 JSON 搜索模式的字段名配置；配置为截取数组或为空时返回 null */
    private static String optJsonFieldName(JSONObject search, String key) {
        Object val = search.opt(key);
        return val instanceof String && !((String) val).trim().isEmpty() ? ((String) val).trim() : null;
    }

    @Override
    public String searchContent(String keyword, boolean quick) {
        
        return searchContent(keyword, quick, "1");
    }

    @Override
    public String searchContent(String keyword, boolean quick, String pg) {
        try {
            fetchRule();
            JSONObject search = rule.optJSONObject("search");
            String searchUrlFlat = rule.optString("search_url", "");

            String page = (pg == null || pg.trim().isEmpty()) ? "1" : pg.trim();
            try {
                if (Integer.parseInt(page) < 1) page = "1";
            } catch (NumberFormatException e) {
                page = "1";
            }

            if ((search == null || !search.has("url")) && searchUrlFlat.isEmpty()) {
                guessSearchUrlIfNeeded();
                if (rule.has("search") && !getRuleVal("search_url").isEmpty()) applyFlatSearchFields();
                initializeSearchConfig();
                search = rule.optJSONObject("search");
                searchUrlFlat = rule.optString("search_url", "");
                if ((search == null || !search.has("url")) && searchUrlFlat.isEmpty()) {
                    // 无搜索 url：返回空列表的合法 JSON，避免上层拿到空串解析失败
                    return wrapList(new JSONArray(), null, page).toString();
                }
            }

            SearchFetchResult fetchResult = fetchSearchContent(keyword, search, searchUrlFlat, page);
            if (fetchResult == null) {
                // 抓取结果为空：返回空列表的合法 JSON，避免上层拿到空串解析失败
                return wrapList(new JSONArray(), null, page).toString();
            }

            String content = fetchResult.content;
            String url = fetchResult.url;

            String searchJsonPath = getRuleVal("searchjsonlist");
            if (!searchJsonPath.isEmpty()) {
                JSONArray jsonVideos = extractVideosByJson(content, searchJsonPath,
                        getRuleVal("searchjsonid"), getRuleVal("searchjsonname"),
                        getRuleVal("searchjsonpic"), getRuleVal("searchjsonnote"));
                if (jsonVideos.length() > 0) return wrapList(jsonVideos, null, page).toString();
            }

            content = RuleUtils.getRegion(content, search);

            String searchTwice = getRuleVal("search_twice");
            if (!searchTwice.isEmpty()) {
                content = applySecondCut(content, applyOrSelector(searchTwice));
            }

            boolean htmlFirst = "1".equals(getRuleVal("search_mode", "0"));
            if (!htmlFirst) {
                String jsonResult = parseSearchResult(content, page);
                if (jsonResult != null && !jsonResult.isEmpty()) return jsonResult;
            }

            inheritVodIdRuleIfNeeded(search);

            if (isCssModeEnabled(search)) {
                SpiderDebug.log("搜索结果使用 CSS/Jsoup 模式提取");
                JSONArray cssVideos = extractSearchResultsByCss(content, search);
                if (cssVideos.length() > 0) return wrapList(cssVideos, content, page).toString();
                SpiderDebug.log("CSS 提取无结果，回退到传统模式");
            }

            String htmlResult = parseHtmlSearchResults(content, search, url, page);

            if (htmlFirst) {
                try {
                    JSONArray arr = new JSONObject(htmlResult).optJSONArray("list");
                    if (arr == null || arr.length() == 0) {
                        String jsonResult = parseSearchResult(content, page);
                        if (jsonResult != null && !jsonResult.isEmpty()) return jsonResult;
                    }
                } catch (Exception ignored) {
                }
            }
            return htmlResult;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        // 异常兜底：返回空列表的合法 JSON（此处 page 不在作用域内，用默认页 "1"）
        try {
            return wrapList(new JSONArray(), null, "1").toString();
        } catch (JSONException je) {
            // 空列表不会触发 JSON 异常，此分支仅作保险，返回等价的空列表 JSON
            return "{\"code\":200,\"msg\":\"ok\",\"page\":\"1\",\"pagecount\":0,\"total\":0,\"limit\":90,\"list\":[]}";
        }
    }

    private static class SearchFetchResult {
        String content;
        String url;
    }

    private SearchFetchResult fetchSearchContent(String keyword, JSONObject search, String searchUrlFlat, String page) throws Exception {
        SearchFetchResult result = new SearchFetchResult();

        if (!searchUrlFlat.isEmpty()) {

            boolean postMode = searchUrlFlat.trim().endsWith(";post") || !getRuleVal("post_data").isEmpty();
            result.url = buildSearchUrl(searchUrlFlat, keyword, page);
            JSONObject headers = parseSearchHeaders(getRuleVal("search_header"));
            result.content = postMode
                    ? executeSearchPost(result.url, keyword, page, headers)
                    : unwrapJsonString(fetchUrl(result.url, headers));
        } else if (search != null && search.has("url")) {
            String rawUrl = search.getString("url");
            boolean postMode = rawUrl.trim().endsWith(";post");
            if (postMode) {
                // 生态惯例：搜索url 末尾 ;post 表示 POST 搜索，标记本身不属于 URL
                rawUrl = rawUrl.trim();
                rawUrl = rawUrl.substring(0, rawUrl.length() - ";post".length());
            }
            result.url = applySearchSuffix(addHttpPrefix(rawUrl)
                    .replace("{wd}", keyword).replace("{pg}", shiftSearchPage(page)));
            JSONObject headers = parseSearchHeaders(getRuleVal("search_header"));
            if (headers == null && search != null) headers = search.optJSONObject("header");
            result.content = postMode || !getRuleVal("post_data").isEmpty()
                    ? executeSearchPost(result.url, keyword, page, headers)
                    : unwrapJsonString(fetchUrl(result.url, headers));
        }
        return result;
    }

    /**
     * POST 搜索执行：body 取「POST请求数据」(post_data)，支持表单 k=v&k=v 与 JSON 字符串两种形态。
     * {wd}/{pg} 在 body 中替换为原始关键词与起始页码（表单由 HTTP 层负责编码）。
     * 生态规范：URL 标记 ;post 即为 POST——body 为空时也发空表单 POST，不回退 GET。
     */
    private String executeSearchPost(String url, String keyword, String page, JSONObject headers) throws Exception {
        if (isInternalUrl(url) || !isSsrfSafe(url)) {
            SpiderDebug.log(safeLog("搜索 POST SSRF blocked: " + url));
            return "";
        }
        applyRequestInterval(url);
        String body = getRuleVal("post_data")
                .replace("{wd}", keyword)
                .replace("{pg}", shiftSearchPage(page));
        Map<String, String> h = mergeHeaders(getHeaders(url), headers);
        String resp;
        if (body.trim().startsWith("{")) {
            resp = OkHttp.post(url, body, h);
            SpiderDebug.log(safeLog("搜索 POST(JSON): " + url));
        } else {
            Map<String, String> form = new LinkedHashMap<>();
            for (String pair : body.split("&")) {
                if (pair.isEmpty()) continue;
                int eq = pair.indexOf('=');
                if (eq > 0) form.put(pair.substring(0, eq), pair.substring(eq + 1));
                else form.put(pair, "");
            }
            resp = OkHttp.post(url, form, h);
            SpiderDebug.log(safeLog("搜索 POST(Form): " + url));
        }
        return unwrapJsonString(convertUnicodeToChinese(resp == null ? "" : resp));
    }

    protected String unwrapJsonString(String body) {
        if (body == null) return body;
        String trimmed = body.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '"' || trimmed.charAt(trimmed.length() - 1) != '"') {
            return body;
        }
        try {
            Object value = new org.json.JSONTokener(trimmed).nextValue();
            if (value instanceof String) {
                return (String) value;
            }
        } catch (Exception e) {
            SpiderDebug.log("unwrapJsonString 解析失败，按原始响应处理");
        }
        return body;
    }

    private String buildSearchUrl(String searchUrlFlat, String keyword, String page) throws Exception {

        if (searchUrlFlat.trim().endsWith(";post")) {
            // 生态惯例：搜索url 末尾 ;post 表示 POST 搜索，标记本身不属于 URL
            searchUrlFlat = searchUrlFlat.trim();
            searchUrlFlat = searchUrlFlat.substring(0, searchUrlFlat.length() - ";post".length());
        }
        String sep = getRuleVal("search_word_sep");
        if (!sep.isEmpty()) keyword = keyword.replace(" ", sep);
        return addHttpPrefix(applySearchSuffix(searchUrlFlat
                .replace("{wd}", URLEncoder.encode(keyword, "UTF-8"))
                .replace("{pg}", shiftSearchPage(page))));
    }

    private String applySearchSuffix(String url) {
        String suffix = getRuleVal("search_suffix");
        if (!suffix.isEmpty() && !url.isEmpty()) {
            return url + suffix;
        }
        return url;
    }

    private JSONObject parseSearchHeaders(String searchHeader) {
        if (searchHeader.isEmpty()) return null;
        return parseHeader(searchHeader);
    }

    private void inheritVodIdRuleIfNeeded(JSONObject search) throws JSONException {
        
        if (search == null) {
            SpiderDebug.log("搜索: 缺少 search 规则对象，跳过 vod_id 规则继承");
            return;
        }
        // 网页截取模式需要「截取数组」；字段名字符串只服务 JSON 模式（且 JSON 解析先于本方法执行），这里统一覆盖
        if (!(search.opt("vod_id") instanceof JSONArray)) {
            JSONObject list = rule.optJSONObject("list");
            if (list != null && list.has("vod_id")) {
                search.put("vod_id", list.getJSONArray("vod_id"));
            } else if (list != null) {
                guessVodIdIfNeeded(list);
                if (list.has("vod_id")) {
                    search.put("vod_id", list.getJSONArray("vod_id"));
                }
            }
        }
        
        if (search.has("vod_id") && "id".equals(search.optString("vod_id", ""))) {
            JSONObject list = rule.optJSONObject("list");
            if (list != null && list.has("vod_id")) {
                search.put("vod_id", list.getJSONArray("vod_id"));
            }
        }
    }

    private String parseHtmlSearchResults(String content, JSONObject search, String url, String page) throws JSONException {
        JSONArray videos = scanSearchNodes(content, search, url);

        // 自动猜测模式：继承的链接规则在搜索页无命中时，用当前搜索页重猜一次
        if (videos.length() == 0 && !content.isEmpty()
                && (guessedVodIdFromHome || search.optJSONArray("vod_id") == null)) {
            JSONArray reGuessed = guessRuleVodId(content);
            if (reGuessed != null && reGuessed.length() >= 5) {
                JSONArray previous = search.optJSONArray("vod_id");
                search.put("vod_id", reGuessed);
                videos = scanSearchNodes(content, search, url);
                if (videos.length() == 0 && previous != null) {
                    search.put("vod_id", previous);
                }
            }
        }

        if (reverseOrder) videos = reverseArray(videos);

        return wrapList(videos, null, page).toString();
    }

    private JSONArray scanSearchNodes(String content, JSONObject search, String url) throws JSONException {
        JSONArray videos = new JSONArray();
        Set<String> seenIds = new HashSet<>();
        int pos = 0;

        JSONArray lookback = search.optJSONArray("search");
        if (lookback == null || RuleUtils.getLookbackCount(lookback) <= 0) {
            lookback = RuleUtils.getLookbackArray(search);
        }

        while (lookback != null) {
            if (videos.length() >= MAX_PAGE_ITEMS) {
                SpiderDebug.log("搜索结果已达单页上限 " + MAX_PAGE_ITEMS + "，截断");
                break;
            }
            int matchPos = content.indexOf(lookback.getString(0), pos);
            if (matchPos == -1) break;

            if (insideNoParseBlock(content, matchPos)) {
                pos = matchPos + 1;
                continue;
            }

            SearchNodeResult nodeResult = extractSearchNode(content, matchPos, lookback, search, url);
            if (nodeResult == null) break;

            if (nodeResult.endPos <= matchPos) {
                pos = matchPos + 1;
            } else {
                pos = nodeResult.endPos;
            }
            String vodId = nodeResult.vodId;

            if (!seenIds.contains(vodId)) {

                if (shouldFilterSearchResult(nodeResult.node, vodId, search)) continue;

                seenIds.add(vodId);
                JSONObject v = buildSearchVideo(nodeResult.node, vodId, search, url);
                videos.put(v);
            }
        }
        return videos;
    }

    private static class SearchNodeResult {
        String node;
        String vodId;
        int endPos;
    }

    private SearchNodeResult extractSearchNode(String content, int pos, JSONArray lookback,
                                                  JSONObject search, String url) throws JSONException {
        List<Integer> urlNodes = null;
        List<Integer> arr = null;
        int blockPos = 0;
        String node = "";
        int lookup = -1;
        int iterations = 0;
        final int MAX_ITERATIONS = 20;

        do {
            
            if (++iterations > MAX_ITERATIONS) {
                SpiderDebug.log(String.format("extractSearchNode 达到最大迭代次数(%d)，当前层级=%d，强制退出", MAX_ITERATIONS, lookback.getInt(4)));
                break;
            }

            arr = HtmlNodeHelper.findUpNodes(content, pos - 1, lookback.getInt(4));
            if (arr.isEmpty()) {
                
                SpiderDebug.log("findUpNodes 未找到祖先节点，跳过当前匹配点");
                return null;
            }
            if (urlNodes == null) {
                urlNodes = arr;
                blockPos = arr.get(arr.size() - 1);
            } else {
                blockPos = RuleUtils.findBlockPos(urlNodes, arr);
            }
            node = HtmlNodeHelper.nodeString(content, blockPos);

            lookup = checkAndAdjustLevelForSearch(node, lookup, lookback, urlNodes, blockPos);
            if (lookup < 0) {
                urlNodes = null;
                blockPos = 0;
                node = "";
            }
        } while (lookup < 0);

        SearchNodeResult result = new SearchNodeResult();
        result.node = node;
        result.vodId = RuleUtils.findSubString(node, 0, search.optJSONArray("vod_id"));
        result.endPos = blockPos + node.length();
        return result;
    }

    private int checkAndAdjustLevelForSearch(String node, int currentLookup, JSONArray lookback,
                                               List<Integer> urlNodes, int blockPos) throws JSONException {
        
        return checkAndAdjustLevel(node, currentLookup, lookback, urlNodes, blockPos);
    }

    private boolean shouldFilterSearchResult(String node, String vodId, JSONObject search) {
        String filterWord = getRuleVal("filter_word");
        if (filterWord.isEmpty()) return false;

        String searchName = RuleUtils.findSubString(node, 0, search.optJSONArray("vod_name"));
        for (String word : filterWord.split("[,，]")) {
            String trimmed = word.trim();
            if (!trimmed.isEmpty() && (vodId.contains(trimmed) || searchName.contains(trimmed))) {
                return true;
            }
        }
        return false;
    }

    private JSONObject buildSearchVideo(String node, String vodId, JSONObject search, String url) throws JSONException {
        JSONObject v = new JSONObject();
        // 与 CSS 搜索分支一致，搜索链接前缀/后缀在打包前拼入
        String sPre = getRuleVal("search_prefix");
        String sSuf = getRuleVal("search_suffix");
        if (!sPre.isEmpty() || !sSuf.isEmpty()) {
            vodId = sPre + vodId + sSuf;
        }
        v.put("vod_id", vodId);
        v.put("vod_name", RuleUtils.findSubString(node, 0, search.optJSONArray("vod_name")));

        String vodPic = addHttpPrefix(RuleUtils.findSubString(node, 0, search.optJSONArray("vod_pic")));
        if (vodPic.isEmpty()) vodPic = guessValueVodPic(node, 0);
        if (vodPic.isEmpty()) vodPic = addHttpPrefix(resolveLazyImage(node));
        if ("1".equals(getRuleVal("PicNeedProxy")) && !vodPic.isEmpty()) {
            vodPic = fixCover(vodPic, url);
        }
        v.put("vod_pic", vodPic);

        v.put("vod_remarks", RuleUtils.findSubString(node, 0, search.optJSONArray("vod_remarks")));

        if (v.getString("vod_name").isEmpty()) {
            v.put("vod_name", guessValueVodName(node, 0));
        }
        if (v.getString("vod_pic").isEmpty()) {
            v.put("vod_pic", guessValueVodPic(node, 0));
        }
        if (v.getString("vod_remarks").isEmpty()) {
            v.put("vod_remarks", guessValueVodRemarks(node, 0, v.getString("vod_name")));
        }

        v.put("vod_id", encodeVodId(v));
        return v;
    }

    private static boolean isSsrfSafe(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            java.net.URL u = new java.net.URL(url);
            String scheme = u.getProtocol().toLowerCase();
            if (SSRF_BLOCKED_SCHEMES.contains(scheme)) return false;
            String host = u.getHost();
            if (host != null) {
                Matcher m = P_INTERNAL_IP.matcher(host);
                if (m.find()) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> applyMirrorHostsUniversal(String url, String mirrorCfg) {
        List<String> result = new ArrayList<>();
        if (url == null || url.isEmpty()) return result;
        result.add(url);
        if (mirrorCfg == null || mirrorCfg.isEmpty()) return result;
        try {
            java.net.URL original = new java.net.URL(url);
            String oldHost = original.getHost();
            int oldPort = original.getPort();
            String path = original.getPath();
            String query = original.getQuery();
            for (String raw : mirrorCfg.split("[,;#]")) {
                String mirror = raw.trim();
                if (mirror.isEmpty()) continue;
                String scheme = original.getProtocol();
                int protoIdx = mirror.indexOf("://");
                if (protoIdx >= 0) {
                    String s = mirror.substring(0, protoIdx).toLowerCase();
                    if (s.equals("http") || s.equals("https")) {
                        scheme = s;
                        mirror = mirror.substring(protoIdx + 3);
                    }
                }
                int slash = mirror.indexOf('/');
                if (slash >= 0) mirror = mirror.substring(0, slash);
                if (mirror.isEmpty()) continue;
                String newHost = mirror;
                int newPort = oldPort;
                if (mirror.contains(":")) {
                    String[] hp = mirror.split(":", 2);
                    newHost = hp[0].trim();
                    try {
                        newPort = Integer.parseInt(hp[1].trim());
                    } catch (Exception e) {
                        newPort = oldPort;
                    }
                }
                if (newHost.isEmpty()) continue;
                if (newHost.equals(oldHost) && newPort == oldPort) continue;
                StringBuilder sb = new StringBuilder();
                sb.append(scheme).append("://").append(newHost);
                if (newPort > 0) sb.append(":").append(newPort);
                sb.append(path);
                if (query != null) sb.append("?").append(query);
                result.add(sb.toString());
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return result;
    }

    private String extractBtwafTokenUniversal(String html, JSONObject headers) {
        if (html == null || html.isEmpty()) return "";
        try {
            Matcher mJson = P_BTWAF_TOKEN_JSON.matcher(html);
            if (mJson.find()) return mJson.group(1);
            Matcher mQuery = P_BTWAF_TOKEN_QUERY.matcher(html);
            if (mQuery.find()) return mQuery.group(1);
            if (headers != null) {
                String cookie = headers.optString("cookie", headers.optString("Cookie", ""));
                if (!cookie.isEmpty()) {
                    Matcher mCookie = P_BTWAF_COOKIE.matcher(cookie);
                    if (mCookie.find()) return mCookie.group(1);
                }
                String setCookie = headers.optString("set-cookie", headers.optString("Set-Cookie", ""));
                if (!setCookie.isEmpty()) {
                    Matcher mSet = P_BTWAF_COOKIE.matcher(setCookie);
                    if (mSet.find()) return mSet.group(1);
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private String followRedirectsUniversal(String html, String baseUrl) {
        if (html == null || html.isEmpty()) return html;
        try {
            for (int depth = 0; depth < 3; depth++) {
                String redirectUrl = null;
                Matcher mMeta = P_META_REFRESH.matcher(html);
                if (mMeta.find()) redirectUrl = mMeta.group(1);
                if (redirectUrl == null) {
                    Matcher mLoc = P_LOCATION_HREF.matcher(html);
                    if (mLoc.find()) redirectUrl = mLoc.group(1);
                }
                if (redirectUrl == null) {
                    Matcher mWin = P_WINDOW_LOCATION.matcher(html);
                    if (mWin.find()) redirectUrl = mWin.group(1);
                }
                if (redirectUrl == null) {
                    Matcher mTimeout = P_SETTIMEOUT_LOCATION.matcher(html);
                    if (mTimeout.find()) redirectUrl = mTimeout.group(1);
                }
                if (redirectUrl == null) break;
                String lower = redirectUrl.toLowerCase();
                if (lower.startsWith("javascript:") || lower.startsWith("#") || lower.startsWith("about:")) break;
                if (!redirectUrl.startsWith("http")) {
                    redirectUrl = resolveRedirectTarget(redirectUrl, baseUrl);
                    if (redirectUrl.isEmpty()) break;
                }
                html = fetchUrl(redirectUrl, null);
                if (html == null || html.isEmpty()) break;
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return html;
    }

    private String resolveRedirectTarget(String target, String baseUrl) {
        if (target == null || target.isEmpty()) return "";
        try {
            if (baseUrl == null || baseUrl.isEmpty()) return addHttpPrefix(target);
            return new java.net.URL(new java.net.URL(baseUrl), target.trim()).toString();
        } catch (Exception e) {
            return addHttpPrefix(target);
        }
    }

    private String decodeResponseUniversal(byte[] body, String encoding, String contentType) {
        if (body == null || body.length == 0) return "";
        try {
            String charset = "";
            if (contentType != null && !contentType.isEmpty()) {
                Pattern pCharset = Pattern.compile("charset=([\\w-]+)", Pattern.CASE_INSENSITIVE);
                Matcher m = pCharset.matcher(contentType);
                if (m.find()) charset = m.group(1);
            }
            if (charset.isEmpty() && body.length >= 3) {
                String head = new String(body, 0, Math.min(body.length, 1024), StandardCharsets.ISO_8859_1);
                Matcher mMeta = P_META_CHARSET.matcher(head);
                if (mMeta.find()) charset = mMeta.group(1);
            }
            if (charset.isEmpty() && body.length >= 3) {
                if ((body[0] & 0xFF) == 0xEF && (body[1] & 0xFF) == 0xBB && (body[2] & 0xFF) == 0xBF) {
                    charset = "UTF-8";
                } else if ((body[0] & 0xFF) == 0xFF && (body[1] & 0xFF) == 0xFE) {
                    charset = "UTF-16LE";
                } else if ((body[0] & 0xFF) == 0xFE && (body[1] & 0xFF) == 0xFF) {
                    charset = "UTF-16BE";
                }
            }
            if (charset.isEmpty() && encoding != null && !encoding.isEmpty()) {
                charset = encoding;
            }
            if (charset.isEmpty()) charset = "UTF-8";
            // 站点声明 gb2312 但正文常含 GBK 扩展字符，按超集 GBK 解码更稳（兼容 gb2312 全部字符）
            if ("gb2312".equalsIgnoreCase(charset) || "gb-2312".equalsIgnoreCase(charset)) charset = "GBK";
            try {
                return new String(body, java.nio.charset.Charset.forName(charset));
            } catch (Exception e) {
                return new String(body, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected String fetchUrl(String url, JSONObject headers) {
        return fetchUrlInternal(url, headers, true);
    }

    private String fetchUrlInternal(String url, JSONObject headers, boolean allowMirror) {
        try {
            
            applyRequestInterval(url);

            url = resolveVariables(url);
            
            url = applyUrlAppend(url);
            
            if (isInternalUrl(url) || !isSsrfSafe(url)) {
                SpiderDebug.log(safeLog("fetchUrl SSRF blocked: " + url));
                failMessage = "SSRF blocked";
                return "";
            }
            Map<String, String> h = getHeaders(url);
            if (headers != null) h = mergeHeaders(h, headers);

            okhttp3.Response resp = OkHttp.newCall(url, h);
            String html;
            try {
                lastResponseCode = resp.code();
                html = decodeResponseBody(resp);
            } finally {
                resp.close();
            }

            html = handleAntiCrawler(url, html, headers);

            if (!isAntiCrawlerPage(html) && isLikelyRedirectPage(html)) {
                html = followRedirectsUniversal(html, url);
            }

            if (isFail(lastResponseCode) && isAntiCrawlerPage(html)) {
                failMessage = "访问失败: " + lastResponseCode;
                if (!requestFailed) Init.show(failMessage);
                requestFailed = true;
                return allowMirror ? tryMirrorHosts(url, headers) : "";
            }
            requestFailed = false;
            failMessage = "";

            return cleanHtmlResponse(html);
        } catch (Exception e) {
            lastResponseCode = 0;
            failMessage = e.getMessage() == null ? "请求异常" : e.getMessage();
            SpiderDebug.log(safeLog("fetchUrl error: " + failMessage));

            // 先尝试镜像域名切换（成功则静默返回，不弹提示）
            if (allowMirror) {
                String mirrorResult = tryMirrorHosts(url, headers);
                if (!mirrorResult.isEmpty()) {
                    requestFailed = false;
                    failMessage = "";
                    return mirrorResult;
                }
            }

            // 镜像失败，利用 retries 配置做本地重试（应对间歇性网络抖动）
            int retryCount = parseIntSafely(getRuleVal("retries"), 0);
            for (int i = 0; i < retryCount; i++) {
                try {
                    long delay = 800L + (long)(Math.random() * 600);
                    Thread.sleep(delay);
                    Map<String, String> retryHeaders = new HashMap<>(getHeaders(url));
                    if (headers != null) {
                        java.util.Iterator<String> it = headers.keys();
                        while (it.hasNext()) {
                            String k = it.next();
                            retryHeaders.put(k, headers.getString(k));
                        }
                    }
                    okhttp3.Response retryResp = OkHttp.newCall(url, retryHeaders);
                    int retryCode;
                    String retryHtml;
                    try {
                        retryCode = retryResp.code();
                        retryHtml = decodeResponseBody(retryResp);
                    } finally {
                        retryResp.close();
                    }
                    if (retryCode >= 200 && retryCode < 400) {
                        SpiderDebug.log(safeLog("fetchUrl 重试成功 (attempt " + (i + 1) + ")"));
                        requestFailed = false;
                        failMessage = "";
                        return cleanHtmlResponse(retryHtml);
                    }
                } catch (Exception retryEx) {
                    SpiderDebug.log(safeLog("fetchUrl 重试失败 (attempt " + (i + 1) + "): " + retryEx.getMessage()));
                }
            }

            // 所有手段均失败才提示用户（requestFailed 防重复弹窗）
            if (!requestFailed) Init.show(failMessage);
            requestFailed = true;
            return "";
        }
    }

    private String decodeResponseBody(okhttp3.Response resp) throws java.io.IOException {
        okhttp3.ResponseBody body = resp.body();
        if (body == null) return "";
        byte[] bytes = body.bytes();
        String charset = getRuleVal("encoding").trim();
        if (!charset.isEmpty()) {
            try {
                return new String(bytes, java.nio.charset.Charset.forName(charset));
            } catch (Exception e) {

                SpiderDebug.log(safeLog("「编码」配置无效(" + charset + ")，回退自动嗅探解码: " + e.getMessage()));
            }
        }
        return decodeResponseUniversal(bytes, "", resp.header("Content-Type"));
    }

    private static final Map<String, Long> LAST_REQUEST_MS = new ConcurrentHashMap<>();

    private void applyRequestInterval(String url) {
        try {
            long interval = Long.parseLong(getRuleVal("request_interval").trim());
            if (interval <= 0) return;
            String host = new java.net.URL(url).getHost();
            long now = System.currentTimeMillis();
            Long last = LAST_REQUEST_MS.get(host);
            if (last != null) {
                long wait = interval - (now - last);
                if (wait > 0) {
                    SpiderDebug.log(safeLog("请求限流: " + host + " 休眠 " + wait + "ms"));
                    Thread.sleep(wait);
                }
            }
            LAST_REQUEST_MS.put(host, System.currentTimeMillis());
        } catch (Exception ignored) {
            
        }
    }

    private String applyUrlAppend(String url) {
        String cfg = getRuleVal("url_append");
        if (cfg.isEmpty() || url == null || url.isEmpty()) return url;
        try {
            if (!url.startsWith("http")) return url;
            String params = cfg
                    .replace("{timestamp}", String.valueOf(System.currentTimeMillis()))
                    .replace("{random}", String.valueOf(
                            java.util.concurrent.ThreadLocalRandom.current().nextInt(100000000)));
            return url + (url.indexOf('?') >= 0 ? "&" : "?") + params;
        } catch (Exception e) {
            SpiderDebug.log(safeLog("applyUrlAppend error: " + e.getMessage()));
            return url;
        }
    }

    private String tryMirrorHosts(String url, JSONObject headers) {
        String cfg = getRuleVal("mirror_hosts");

        String fallback = variableMap.getOrDefault("主页url-c", "");
        if (fallback.isEmpty()) fallback = getRuleVal("home_url_c");
        if (!fallback.isEmpty()) cfg = cfg.isEmpty() ? fallback : cfg + ";" + fallback;
        if (cfg.isEmpty() || url == null || url.isEmpty()) return "";
        try {
            List<String> candidates = applyMirrorHostsUniversal(url, cfg);
            for (int i = 1; i < candidates.size(); i++) {
                String candidate = candidates.get(i);
                SpiderDebug.log(safeLog("镜像域名切换: " + candidate));
                String result = fetchUrlInternal(candidate, headers, false);
                if (!result.isEmpty()) return result;
            }
        } catch (Exception e) {
            SpiderDebug.log(safeLog("tryMirrorHosts error: " + e.getMessage()));
        }
        return "";
    }

    private String cleanHtmlResponse(String html) {
        
        return P_HTML_COMMENT.matcher(P_INVISIBLE.matcher(html).replaceAll(""))
                .replaceAll("")
                .replace("\r\n", "")
                .replace("\n", "");
    }

    protected String convertUnicodeToChinese(String str) {
        if (str == null || !str.contains("\\u")) return str;
        try {
            Matcher matcher = P_UNICODE_SEQ.matcher(str);
            while (matcher.find()) {
                String unicodeNum = matcher.group(2);
                char c = (char) Integer.parseInt(unicodeNum, 16);
                str = str.replace(matcher.group(1), String.valueOf(c));
            }
            return str;
        } catch (Exception e) {
            
            return str;
        }
    }

    protected String fetch(String webUrl) {
        if (isInternalUrl(webUrl)) {
            SpiderDebug.log(safeLog("fetch SSRF blocked: " + webUrl));
            return "";
        }
        String html = OkHttp.string(webUrl, getHeaders(webUrl));
        html = handleAntiCrawler(webUrl, html, null);
        html = convertUnicodeToChinese(html);
        return cleanHtmlResponse(html);
    }

    protected String extractField(String block, String rule) {
        if (rule == null || rule.isEmpty()) return "";
        if (rule.contains("&&")) {
            String[] se = rule.split("&&", 2);
            List<String> r = subContent(block, se[0], se[1]);
            return r.isEmpty() ? "" : cleanHtml(r.get(0));
        }
        return cleanHtml(rule);
    }

    protected static String cleanHtml(String s) {
        if (s == null) return "";
        String r = P_HTML_TAG.matcher(s).replaceAll("");
        r = P_HTML_ENTITY.matcher(r).replaceAll("");
        r = P_RESIDUAL_SYMS.matcher(r).replaceAll("");
        r = P_WHITESPACE.matcher(r).replaceAll(" ");
        return r.trim();
    }

    protected String encodeVodId(JSONObject item) {
        try {
            return Base64.encodeToString(item.toString().getBytes(StandardCharsets.UTF_8), BASE64_FLAG);
        } catch (Exception e) {
            SpiderDebug.log("encodeVodId error: " + e.getMessage());
            return "";
        }
    }

    private static final int MAX_ANTI_CRAWLER_RETRY = 5;

    private long antiCrawlDeadline = 0;

    private static final long ANTI_CRAWLER_DELAY_MS = 1500;
    
    private static final long REFRESH_WAIT_DELAY_MS = 2300;

    private static final List<String> CF_DETECT_KEYWORDS = Arrays.asList(
            "cf-browser-verification", "cf-challenge", "cf_clearance",
            "Just a moment...", "Checking your browser",
            "_cf_chl", "__cf_bm", "challenge-platform",
            "turnstile"
    );

    private static final List<String> BT_DETECT_KEYWORDS = Arrays.asList(
            "btwaf", "检测中", "跳转中", "安全检测",
            "yanzheng_huadong", "huadong_", "/cdn-cgi/"
    );

    private static final List<String> SLIDER_DETECT_KEYWORDS = Arrays.asList(
            "滑动验证", "滑块验证", "huadong_", "click_captcha",
            "slider-verify", "geetest", "captcha",
            "slider-box", "sliderBtn", "向右滑动"
    );

    // ---- FlareSolverr Cloudflare 绕过相关常量与状态 ----
    private static final long FS_SESSION_TTL_MS = 10 * 60 * 1000L;   // FlareSolverr 会话活跃缓存 10 分钟
    private static final String CF_CACHE_PREFS = "cf_clearance_cache"; // 持久化 cf_clearance cookie 的 SharedPreferences 名称

    private volatile boolean fsSessionActive = false; // 最近一次通过 FlareSolverr 获取 cf cookie 的会话是否仍然活跃
    private volatile long fsSessionTime = 0L;         // 会话活跃标记时间戳（elapsedRealtime）

    private long antiCrawlBudget() {
        long budget = 0;
        try {
            budget = Long.parseLong(getRuleVal("antiCrawlTimeout", "0").trim());
        } catch (Exception ignored) {
        }
        if (budget <= 0) budget = 20000;
        return Math.max(5000, Math.min(60000, budget));
    }

    protected String handleAntiCrawler(String webUrl, String html) {
        try {
            if (html == null || html.isEmpty()) return html;

            if (!isAntiCrawlerPage(html)) return html;

            antiCrawlDeadline = SystemClock.elapsedRealtime() + antiCrawlBudget();

            SpiderDebug.log(String.format("检测到反爬保护: %s", detectAntiCrawlerType(html)));

            if (isCloudflarePage(html)) {
                html = bypassCloudflare(webUrl, html);
                if (!isAntiCrawlerPage(html)) return html;
            }

            if (isBaoTaWafPage(html)) {
                html = bypassBaoTaWaf(webUrl, html);
                if (!isAntiCrawlerPage(html)) return html;
            }

            if (isSliderVerifyPage(html)) {
                boolean handled = handleSliderVerify(webUrl, html);
                if (handled) {
                    html = fetchUrl(webUrl, rule.optJSONObject("header"));
                    if (!isAntiCrawlerPage(html)) return html;
                }
            }

            if (isRefreshWaitPage(html)) {
                html = bypassRefreshWait(webUrl, html, null);
                if (!isAntiCrawlerPage(html)) return html;
            }

            for (int i = 0; i < MAX_ANTI_CRAWLER_RETRY; i++) {
                if (SystemClock.elapsedRealtime() > antiCrawlDeadline) {
                    SpiderDebug.log("反爬重试超出总预算，提前结束");
                    break;
                }
                Thread.sleep(ANTI_CRAWLER_DELAY_MS);
                html = fetchUrlWithRetry(webUrl);
                if (!isAntiCrawlerPage(html)) break;
                SpiderDebug.log(String.format("反爬重试 %d/%d", i + 1, MAX_ANTI_CRAWLER_RETRY));
            }
        } catch (Exception e) {
            SpiderDebug.log("反爬处理异常: " + e.getMessage());
        }
        return html;
    }

    protected String handleAntiCrawler(String webUrl, String html, JSONObject customHeaders) {
        try {
            if (html == null || html.isEmpty()) return html;
            if (!isAntiCrawlerPage(html)) return html;

            antiCrawlDeadline = SystemClock.elapsedRealtime() + antiCrawlBudget();

            if (isCloudflarePage(html)) {
                html = bypassCloudflare(webUrl, html, customHeaders);
            } else if (isBaoTaWafPage(html)) {
                html = bypassBaoTaWaf(webUrl, html, customHeaders);
            } else if (isSliderVerifyPage(html)) {
                handleSliderVerify(webUrl, html);
                html = fetchUrl(webUrl, customHeaders);
            } else if (isRefreshWaitPage(html)) {
                html = bypassRefreshWait(webUrl, html, customHeaders);
            }

            if (isAntiCrawlerPage(html)) {
                html = handleAntiCrawler(webUrl, html); 
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return html;
    }

    protected boolean isAntiCrawlerPage(String html) {
        if (html == null || html.isEmpty()) return false;
        return isCloudflarePage(html) || isBaoTaWafPage(html)
                || isSliderVerifyPage(html) || isGenericBlockPage(html)
                || isRefreshWaitPage(html);
    }

    private boolean isLikelyRedirectPage(String html) {
        if (html == null) return false;
        if (P_META_REFRESH.matcher(html).find()) return true;
        String trimmed = html.trim();
        if (trimmed.isEmpty() || trimmed.length() > 2048) return false;
        String lower = trimmed.toLowerCase();
        return lower.contains("location.href") || lower.contains("window.location")
                || lower.contains("location.replace(") || lower.contains("settimeout");
    }

    protected boolean isRefreshWaitPage(String html) {
        if (html == null || html.isEmpty()) return false;
        return html.contains("页面加载中") && html.contains("location.reload");
    }

    protected String bypassRefreshWait(String webUrl, String html, JSONObject customHeaders) {
        try {
            for (int i = 0; i < MAX_ANTI_CRAWLER_RETRY; i++) {
                if (SystemClock.elapsedRealtime() > antiCrawlDeadline) {
                    SpiderDebug.log("等待重载盾重试超出反爬总预算，提前结束");
                    break;
                }
                Thread.sleep(REFRESH_WAIT_DELAY_MS);
                Map<String, String> headers = getHeaders(webUrl);
                if (customHeaders != null && customHeaders.length() > 0) {
                    headers = mergeHeaders(headers, customHeaders);
                }
                okhttp3.Response resp = OkHttp.newCall(webUrl, headers);
                int code;
                String body;
                try {
                    code = resp.code();
                    extractAllCookies(resp);
                    body = resp.body().string();
                } finally {
                    resp.close();
                }
                lastResponseCode = code;
                html = body;
                SpiderDebug.log(String.format("等待重载盾重试 %d/%d: %d", i + 1, MAX_ANTI_CRAWLER_RETRY, code));
                if (!isRefreshWaitPage(html)) return html;
            }
        } catch (Exception e) {
            SpiderDebug.log("等待重载盾绕过异常: " + e.getMessage());
        }
        return html;
    }

    protected boolean isCloudflarePage(String html) {
        if (html == null || html.isEmpty()) return false;
        for (String keyword : CF_DETECT_KEYWORDS) {
            if (html.toLowerCase().contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    protected boolean isBaoTaWafPage(String html) {
        if (html == null || html.isEmpty()) return false;
        for (String keyword : BT_DETECT_KEYWORDS) {
            if (html.contains(keyword)) return true;
        }
        return false;
    }

    protected boolean isSliderVerifyPage(String html) {
        if (html == null || html.isEmpty()) return false;
        
        try {
            if (SliderVerifyUtils.isSliderVerifyPage(html)) return true;
        } catch (Exception e) {
            SpiderDebug.log("滑块页面检测异常，回退到关键词匹配: " + e.getMessage());
        }
        for (String keyword : SLIDER_DETECT_KEYWORDS) {
            if (html.contains(keyword)) return true;
        }
        return false;
    }

    protected boolean isGenericBlockPage(String html) {
        if (html == null || html.isEmpty()) return false;
        String lowerHtml = html.toLowerCase();
        
        return lowerHtml.contains("访问频率")
                || lowerHtml.contains("请求过于频繁")
                || (lowerHtml.contains("403 forbidden") && lowerHtml.contains("被拦截"))
                || (lowerHtml.contains("access denied")
                    && (lowerHtml.contains("waf") || lowerHtml.contains("firewall")));
    }

    protected String detectAntiCrawlerType(String html) {
        if (isCloudflarePage(html)) return "Cloudflare";
        if (isBaoTaWafPage(html)) return "宝塔WAF";
        if (isSliderVerifyPage(html)) return "滑块验证";
        if (isRefreshWaitPage(html)) return "等待重载盾";
        if (isGenericBlockPage(html)) return "通用拦截";
        return "未知";
    }

    protected String bypassCloudflare(String webUrl, String html) {
        return bypassCloudflare(webUrl, html, null);
    }

    protected String bypassCloudflare(String webUrl, String html, JSONObject customHeaders) {
        try {
            SpiderDebug.log("尝试绕过 Cloudflare 保护...");

            Map<String, String> headers = customHeaders != null
                    ? mergeHeaders(getHeaders(webUrl), customHeaders)
                    : getHeaders(webUrl);

            // 优先使用 FlareSolverr（配置了 flare_solver_url 时）绕过 CF，比单纯改头/等待更可靠
            if (!getFlareSolverrUrl().isEmpty()) {
                String fsHtml = bypassCloudflareWithFlareSolverr(webUrl, html);
                if (fsHtml != null && !isCloudflarePage(fsHtml)) {
                    SpiderDebug.log("Cloudflare 绕过成功 (FlareSolverr)");
                    return fsHtml;
                }
                if (fsHtml != null && !isAntiCrawlerPage(fsHtml)) {
                    return fsHtml;
                }
            }

            // 兜底：直连增强请求头 + 等待重载
            headers = enhanceForCloudflare(headers);
            // 优先复用同域会话（cookie 与匹配 UA），避免 UA 与 cf_clearance 不一致导致再次拦截
            Map<String, String> cfCtx = getCfDomainContext(webUrl);
            if (cfCtx != null) {
                headers.put("Cookie", Util.mergeCookies(headers.getOrDefault("Cookie", ""), cfCtx.get("cookie")));
                String cfUa = cfCtx.get("ua");
                if (!cfUa.isEmpty()) {
                    headers.put("User-Agent", cfUa);
                }
            } else {
                Map<String, String> gCtx = getCachedCfContext();
                if (!gCtx.get("cookie").isEmpty()) {
                    headers.put("Cookie", Util.mergeCookies(headers.getOrDefault("Cookie", ""), gCtx.get("cookie")));
                    if (!gCtx.get("ua").isEmpty()) {
                        headers.put("User-Agent", gCtx.get("ua"));
                    }
                }
            }

            Thread.sleep(2000 + (long)(Math.random() * 2000));

            okhttp3.Response cfResp = null;
            try {
                cfResp = OkHttp.newCall(webUrl, headers);
                extractAllCookies(cfResp);
            } catch (Exception cfEx) {
                SpiderDebug.log("Cloudflare 预访问异常: " + cfEx.getMessage());
            } finally {
                if (cfResp != null) cfResp.close();
            }

            Thread.sleep(3000);

            html = OkHttp.string(webUrl, headers);

            if (!isCloudflarePage(html)) {
                SpiderDebug.log("Cloudflare 绕过成功");
            }
        } catch (Exception e) {
            SpiderDebug.log("Cloudflare 绕过失败: " + e.getMessage());
        }
        return html;
    }

    // ==================== FlareSolverr Cloudflare 绕过 ====================

    /** 读取 FlareSolverr 服务地址（rule: flare_solver_url）。未配置返回空串。 */
    private String getFlareSolverrUrl() {
        return getRuleVal("flare_solver_url");
    }

    /** FlareSolverr 会话名（rule: flare_solver_session，默认 "pianku"）。 */
    private String getFlareSolverrSession() {
        return getRuleVal("flare_solver_session", "pianku");
    }

    /** FlareSolverr 单次请求超时毫秒（rule: flare_solver_timeout，默认 45000）。 */
    private int getFlareSolverrTimeout() {
        String v = getRuleVal("flare_solver_timeout");
        try {
            int t = Integer.parseInt(v.trim());
            if (t > 0) return t;
        } catch (Exception ignored) {
        }
        return 45000;
    }

    /** cf cookie 手动覆盖（rule: cf_cookie）。设置后跳过自动获取。 */
    private String cfManualCookie() {
        return getRuleVal("cf_cookie");
    }

    /** 是否启用自动 cf 获取（rule: cf_auto，默认 true；"0"/"false" 关闭）。 */
    private boolean cfAuto() {
        String v = getRuleVal("cf_auto");
        return !("0".equals(v) || "false".equalsIgnoreCase(v));
    }

    /** cf_clearance cookie 缓存 TTL 秒数（rule: cf_cache_ttl，默认 21600）。 */
    private int getCfCacheTtlSeconds() {
        String v = getRuleVal("cf_cache_ttl");
        try {
            int t = Integer.parseInt(v.trim());
            if (t > 0) return t;
        } catch (Exception ignored) {
        }
        return 21600;
    }

    private SharedPreferences cfPrefs() {
        Context ctx = getContext();
        if (ctx == null) return null;
        try {
            return ctx.getSharedPreferences(CF_CACHE_PREFS, Context.MODE_PRIVATE);
        } catch (Exception e) {
            SpiderDebug.log("读取 cf 缓存 SharedPreferences 异常: " + e.getMessage());
            return null;
        }
    }

    /** 读取可用的 cf 缓存（cookie + 匹配 UA）：优先手动覆盖（无 UA），其次带 TTL 的持久化缓存。无则返回空 cookie。 */
    private Map<String, String> getCachedCfContext() {
        Map<String, String> m = new HashMap<>();
        m.put("cookie", "");
        m.put("ua", "");
        String manual = cfManualCookie();
        if (!manual.isEmpty()) {
            m.put("cookie", manual);
            return m;
        }
        if (!cfAuto()) return m;
        SharedPreferences prefs = cfPrefs();
        if (prefs == null) return m;
        try {
            String raw = prefs.getString("cf_clearance", "");
            if (raw.isEmpty()) return m;
            String cookie;
            String ua = "";
            long ts = 0L;
            if (raw.trim().startsWith("{")) {
                // 新格式：JSON {cookie, ua, ts}
                JSONObject json = new JSONObject(raw);
                cookie = json.optString("cookie", "");
                ua = json.optString("ua", "");
                ts = json.optLong("ts", 0L);
            } else {
                // 旧格式：cookie \u0001 timestamp（无 UA）
                int sep = raw.indexOf('\u0001');
                cookie = sep >= 0 ? raw.substring(0, sep) : raw;
                if (sep >= 0) {
                    try {
                        ts = Long.parseLong(raw.substring(sep + 1).trim());
                    } catch (Exception ignored) {
                    }
                }
            }
            long ttlMs = getCfCacheTtlSeconds() * 1000L;
            if (ts > 0 && System.currentTimeMillis() - ts > ttlMs) {
                m.put("cookie", "");
                m.put("ua", "");
                return m;
            }
            m.put("cookie", cookie);
            m.put("ua", ua);
        } catch (Exception e) {
            SpiderDebug.log("读取 cf 缓存异常: " + e.getMessage());
        }
        return m;
    }

    /** 持久化 cf 缓存（cookie + 匹配 UA + 时间戳，JSON，供 TTL 校验）。 */
    private void setCachedCfCookie(String cookie, String ua) {
        if (cookie == null || cookie.isEmpty()) return;
        SharedPreferences prefs = cfPrefs();
        if (prefs == null) return;
        try {
            JSONObject json = new JSONObject();
            json.put("cookie", cookie);
            json.put("ua", ua == null ? "" : ua);
            json.put("ts", System.currentTimeMillis());
            prefs.edit().putString("cf_clearance", json.toString()).commit();
        } catch (Exception e) {
            SpiderDebug.log("写入 cf 缓存异常: " + e.getMessage());
        }
    }

    /** cf 域名会话缓存 TTL 秒数（rule: cf_session_ttl，默认 1800）。 */
    private int getCfSessionTtlSeconds() {
        String v = getRuleVal("cf_session_ttl");
        try {
            int t = Integer.parseInt(v.trim());
            if (t > 0) return t;
        } catch (Exception ignored) {
        }
        return 1800;
    }

    /** 取 URL 的 host 作为会话缓存键。 */
    private String cfDomain(String webUrl) {
        try {
            return new java.net.URL(webUrl).getHost();
        } catch (Exception e) {
            return "";
        }
    }

    /** 读取同域 CF 会话（cookie + 匹配 UA）。无或超期返回 null。 */
    private Map<String, String> getCfDomainContext(String webUrl) {
        String domain = cfDomain(webUrl);
        if (domain.isEmpty()) return null;
        SharedPreferences prefs = cfPrefs();
        if (prefs == null) return null;
        try {
            String raw = prefs.getString("cf_ctx:" + domain, "");
            if (raw.isEmpty()) return null;
            JSONObject json = new JSONObject(raw);
            long ts = json.optLong("ts", 0L);
            long ttlMs = getCfSessionTtlSeconds() * 1000L;
            if (ts > 0 && System.currentTimeMillis() - ts > ttlMs) {
                return null;
            }
            String cookie = json.optString("cookie", "");
            if (cookie.isEmpty()) return null;
            Map<String, String> m = new HashMap<>();
            m.put("cookie", cookie);
            m.put("ua", json.optString("ua", ""));
            return m;
        } catch (Exception e) {
            SpiderDebug.log("读取 cf 域会话异常: " + e.getMessage());
            return null;
        }
    }

    /** 写入同域 CF 会话（cookie + 匹配 UA + 时间戳）。 */
    private void setCfDomainContext(String webUrl, String cookie, String ua) {
        if (cookie == null || cookie.isEmpty()) return;
        String domain = cfDomain(webUrl);
        if (domain.isEmpty()) return;
        SharedPreferences prefs = cfPrefs();
        if (prefs == null) return;
        try {
            JSONObject json = new JSONObject();
            json.put("cookie", cookie);
            json.put("ua", ua == null ? "" : ua);
            json.put("ts", System.currentTimeMillis());
            prefs.edit().putString("cf_ctx:" + domain, json.toString()).commit();
        } catch (Exception e) {
            SpiderDebug.log("写入 cf 域会话异常: " + e.getMessage());
        }
    }

    /** FlareSolverr 会话是否仍在活跃窗口内。 */
    private boolean checkFsSessionActive() {
        return fsSessionActive
                && SystemClock.elapsedRealtime() - fsSessionTime < FS_SESSION_TTL_MS;
    }

    private void markFsSessionActive() {
        fsSessionActive = true;
        fsSessionTime = SystemClock.elapsedRealtime();
    }

    /**
     * 使用 FlareSolverr 绕过 Cloudflare 并取回页面正文。
     * 优先命中缓存 / 活跃会话；否则调用 FlareSolverr 获取 cf_clearance 后直取。
     * 若结果仍是滑块验证页，则再走一次 verify_check 提交。
     */
    private String bypassCloudflareWithFlareSolverr(String webUrl, String html) {
        try {
            // 命中同域会话（cookie + 匹配 UA）：直接直连取回，省掉一次 FlareSolverr 调用
            Map<String, String> ctx = getCfDomainContext(webUrl);
            if (ctx != null) {
                Map<String, String> directHeaders = new HashMap<>();
                String ctxUa = ctx.get("ua");
                directHeaders.put("User-Agent", ctxUa.isEmpty() ? UA_PC : ctxUa);
                directHeaders.put("Cookie", ctx.get("cookie"));
                directHeaders.put("Referer", webUrl);
                String directBody = OkHttp.string(webUrl, directHeaders);
                if (directBody != null && !directBody.isEmpty()
                        && !isCloudflarePage(directBody) && !isSliderVerifyPage(directBody)) {
                    SpiderDebug.log("CF 同域会话命中，直连取回成功");
                    return directBody;
                }
            }

            // 会话活跃：直接让 FlareSolverr 代取，避免重复解盾
            if (checkFsSessionActive()) {
                String body = flareSolverrGet(webUrl);
                if (body != null && !isSliderVerifyPage(body)) {
                    markFsSessionActive();
                    return body;
                }
            }

            Map<String, String> result = ensureCfCookie(webUrl);
            if (result == null) return null;
            String body = result.get("body");
            if (body == null || body.isEmpty()) return html;
            markFsSessionActive();

            if (isSliderVerifyPage(body)) {
                SpiderDebug.log("FlareSolverr 结果仍为滑块验证页，提交 verify_check...");
                String verified = postVerifyCheckWithFlareSolverr(webUrl);
                if (verified != null && !isAntiCrawlerPage(verified)) {
                    return verified;
                }
            }
            return body;
        } catch (Exception e) {
            SpiderDebug.log("FlareSolverr 绕过异常: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取可用的 cf cookie 与绕过后的页面正文。
     * @return Map: cookie -> cf cookie 串；body -> FlareSolverr 代取的页面正文；flareResult -> "1"/""。
     *         无法获取时返回 null。
     */
    private Map<String, String> ensureCfCookie(String targetUrl) {
        // 手动 cookie：直接带 cookie 直取，不走 FlareSolverr
        String manual = cfManualCookie();
        if (!manual.isEmpty()) {
            String body = "";
            try {
                Map<String, String> h = new HashMap<>();
                h.put("User-Agent", UA_PC);
                h.put("Cookie", manual);
                body = OkHttp.string(targetUrl, h);
            } catch (Exception e) {
                SpiderDebug.log("手动 cf cookie 直取异常: " + e.getMessage());
            }
            Map<String, String> r = new HashMap<>();
            r.put("cookie", manual);
            r.put("body", body);
            r.put("ua", "");
            r.put("flareResult", "");
            return r;
        }

        // 无手动 cookie：调用 FlareSolverr 解盾，成功后写全局/域缓存（cookie + 匹配 UA）
        Map<String, String> fs = fetchCfClearanceWithFlareSolverr(targetUrl);
        if (fs == null) return null;
        setCachedCfCookie(fs.get("cookie"), fs.get("ua"));
        setCfDomainContext(targetUrl, fs.get("cookie"), fs.get("ua"));
        return fs;
    }

    /** 调用 FlareSolverr 解盾并提取 cf_clearance cookie。失败返回 null。 */
    private Map<String, String> fetchCfClearanceWithFlareSolverr(String targetUrl) {
        String fsUrl = getFlareSolverrUrl();
        if (fsUrl.isEmpty()) return null;
        try {
            JSONObject req = new JSONObject();
            req.put("cmd", "request.get");
            req.put("url", targetUrl);
            req.put("maxTimeout", getFlareSolverrTimeout());
            req.put("session", getFlareSolverrSession());

            String resp = OkHttp.post(fsUrl, req.toString(), new HashMap<>());
            if (resp == null || resp.isEmpty()) return null;

            JSONObject sol = new JSONObject(resp);
            if (!"ok".equals(sol.optString("status"))) {
                SpiderDebug.log("FlareSolverr 返回非 ok 状态: " + sol.optString("status"));
                return null;
            }
            JSONObject solution = sol.optJSONObject("solution");
            if (solution == null) return null;

            String cookies = "";
            JSONArray cookiesArray = solution.optJSONArray("cookies");
            if (cookiesArray != null) {
                cookies = fsCookiesToString(cookiesArray);
            }
            String cfClearance = findCfClearance(cookiesArray);
            if (cfClearance.isEmpty() && cookies.isEmpty()) {
                SpiderDebug.log("FlareSolverr 未返回 cf_clearance");
                return null;
            }

            Map<String, String> r = new HashMap<>();
            r.put("cookie", cookies.isEmpty() ? ("cf_clearance=" + cfClearance) : cookies);
            r.put("body", solution.optString("response", ""));
            r.put("ua", solution.optString("userAgent", ""));
            r.put("flareResult", "1");
            return r;
        } catch (Exception e) {
            SpiderDebug.log("FlareSolverr 请求异常: " + e.getMessage());
        }
        return null;
    }

    /** 将 FlareSolverr 返回的 cookies 数组拼接为 "k=v; k2=v2" 串。 */
    private String fsCookiesToString(JSONArray cookiesArray) {
        if (cookiesArray == null) return "";
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i < cookiesArray.length(); i++) {
                Object item = cookiesArray.opt(i);
                String nv;
                if (item instanceof JSONObject) {
                    JSONObject jo = (JSONObject) item;
                    nv = jo.optString("name", "") + "=" + jo.optString("value", "");
                } else {
                    nv = String.valueOf(item);
                }
                if (nv.isEmpty()) continue;
                if (sb.length() > 0) sb.append("; ");
                sb.append(nv);
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    /** 从 cookies 数组中查找 cf_clearance 值。 */
    private String findCfClearance(JSONArray cookiesArray) {
        if (cookiesArray == null) return "";
        try {
            for (int i = 0; i < cookiesArray.length(); i++) {
                Object item = cookiesArray.opt(i);
                if (item instanceof JSONObject) {
                    JSONObject jo = (JSONObject) item;
                    if ("cf_clearance".equals(jo.optString("name", ""))) {
                        return jo.optString("value", "");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** 让 FlareSolverr 代取某 URL 的页面正文（走当前活跃会话）。 */
    private String flareSolverrGet(String targetUrl) {
        String fsUrl = getFlareSolverrUrl();
        if (fsUrl.isEmpty()) return null;
        try {
            JSONObject req = new JSONObject();
            req.put("cmd", "request.get");
            req.put("url", targetUrl);
            req.put("maxTimeout", getFlareSolverrTimeout());
            req.put("session", getFlareSolverrSession());

            String resp = OkHttp.post(fsUrl, req.toString(), new HashMap<>());
            if (resp == null || resp.isEmpty()) return null;
            JSONObject sol = new JSONObject(resp);
            if (!"ok".equals(sol.optString("status"))) return null;
            JSONObject solution = sol.optJSONObject("solution");
            if (solution == null) return null;
            return solution.optString("response", "");
        } catch (Exception e) {
            SpiderDebug.log("FlareSolverr 代取异常: " + e.getMessage());
        }
        return null;
    }

    /** CF 绕过结果仍是滑块页时，通过 FlareSolverr POST verify_check.php 提交 pass=1，再代取页面。 */
    private String postVerifyCheckWithFlareSolverr(String webUrl) {
        try {
            String fsUrl = getFlareSolverrUrl();
            if (fsUrl.isEmpty()) return null;

            String verifyUrl = getRuleVal("cf_verify_url");
            if (verifyUrl.isEmpty()) {
                verifyUrl = siteBase(webUrl) + "/verify_check.php";
            }

            JSONObject postReq = new JSONObject();
            postReq.put("cmd", "request.post");
            postReq.put("url", verifyUrl);
            postReq.put("postData", "pass=1");
            postReq.put("maxTimeout", 15000);
            postReq.put("session", getFlareSolverrSession());
            OkHttp.post(fsUrl, postReq.toString(), new HashMap<>());

            return flareSolverrGet(webUrl);
        } catch (Exception e) {
            SpiderDebug.log("FlareSolverr verify_check 异常: " + e.getMessage());
        }
        return null;
    }

    /** 取 URL 的 scheme://host[:port] 基础部分。 */
    private String siteBase(String webUrl) {
        try {
            java.net.URL u = new java.net.URL(webUrl);
            StringBuilder sb = new StringBuilder();
            sb.append(u.getProtocol()).append("://").append(u.getHost());
            if (u.getPort() != -1) sb.append(":").append(u.getPort());
            return sb.toString();
        } catch (MalformedURLException e) {
            return webUrl;
        }
    }

    private Map<String, String> enhanceForCloudflare(Map<String, String> headers) {
        
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Cache-Control", "max-age=0");
        headers.put("Upgrade-Insecure-Requests", "1");
        
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("Sec-Fetch-User", "?1");

        String ua = headers.getOrDefault("User-Agent", "");
        if (ua.contains("Mobile") || ua.isEmpty()) {
            headers.put("User-Agent", UA_PC);
        }
        return headers;
    }

    protected String bypassBaoTaWaf(String webUrl, String html) {
        return bypassBaoTaWaf(webUrl, html, null);
    }

    protected String bypassBaoTaWaf(String webUrl, String html, JSONObject customHeaders) {
        try {
            if (!rule.optBoolean("btwaf", false) && !isBaoTaWafPage(html)) return html;

            SpiderDebug.log("尝试绕过宝塔 WAF 防护...");

            Map<String, String> headers = customHeaders != null
                    ? mergeHeaders(getHeaders(webUrl), customHeaders)
                    : getHeaders(webUrl);

            for (int i = 0; i < MAX_ANTI_CRAWLER_RETRY; i++) {
                if (!isBaoTaWafPage(html)) break;

                String btwafToken = extractBtwafTokenEnhanced(html);
                if (!btwafToken.isEmpty()) {
                    String btUrl = appendQueryParam(webUrl, "btwaf", btwafToken);
                    okhttp3.Response resp = null;
                    try {
                        resp = OkHttp.newCall(btUrl, headers);
                        extractAllCookies(resp);
                    } catch (Exception e) {
                        SpiderDebug.log("bypassBaoTaWaf btwaf 请求异常: " + e.getMessage());
                    } finally {
                        if (resp != null) resp.close();
                    }

                    Thread.sleep(ANTI_CRAWLER_DELAY_MS);
                    html = OkHttp.string(webUrl, headers);
                    continue;
                }

                String redirectUrl = extractRedirectUrl(html);
                if (!redirectUrl.isEmpty()) {
                    OkHttp.string(redirectUrl, headers);
                    Thread.sleep(ANTI_CRAWLER_DELAY_MS);
                    html = OkHttp.string(webUrl, headers);
                    continue;
                }

                Thread.sleep(ANTI_CRAWLER_DELAY_MS + i * 500);
                html = OkHttp.string(webUrl, headers);
            }

            if (!isBaoTaWafPage(html)) {
                SpiderDebug.log("宝塔 WAF 绕过成功");
            }
        } catch (Exception e) {
            SpiderDebug.log("宝塔 WAF 绕过异常: " + e.getMessage());
        }
        return html;
    }

    private String extractBtwafTokenEnhanced(String html) {

        JSONObject hdr = null;
        try {
            hdr = headerObject();
        } catch (Exception ignored) {
        }
        return extractBtwafTokenUniversal(html, hdr);
    }

    private String extractRedirectUrl(String html) {
        
        Pattern p1 = P_META_REFRESH;
        Matcher m1 = p1.matcher(html);
        if (m1.find()) return m1.group(1).trim();

        Pattern p2 = P_LOCATION_HREF;
        Matcher m2 = p2.matcher(html);
        if (m2.find()) return m2.group(1).trim();

        Pattern p3 = P_WINDOW_LOCATION;
        Matcher m3 = p3.matcher(html);
        if (m3.find()) return m3.group(1).trim();

        Pattern p4 = P_SETTIMEOUT_LOCATION;
        Matcher m4 = p4.matcher(html);
        if (m4.find()) return m4.group(1).trim();

        return "";
    }

    protected boolean handleSliderVerify(String webUrl, String html) {
        try {
            SpiderDebug.log("检测到滑块验证，开始处理...");

            SliderVerifyUtils verifier = createSliderVerifier(webUrl);

            configureSliderVerifier(verifier);

            boolean success;
            if (verifier.isVerifyPage(html)) {
                SpiderDebug.log("检测到滑块验证页面，尝试自动验证...");
                String verifiedHtml = verifier.requestWithVerify(webUrl);
                success = verifiedHtml != null && !verifiedHtml.isEmpty()
                        && !verifier.isVerifyPage(verifiedHtml);
                if (success) {
                    mergeVerifyCookie(verifier);
                }
            } else {
                success = true;
            }

            return success;
        } catch (Exception e) {
            SpiderDebug.log("滑块验证处理异常: " + e.getMessage());
        }
        return false;
    }

    private SliderVerifyUtils createSliderVerifier(String siteUrl) {
        SliderVerifyUtils verifier = new SliderVerifyUtils(siteUrl);

        String jsKeyUrl = rule.optString("js_key_url", "");
        if (!jsKeyUrl.isEmpty()) {
            verifier.setJsKeyUrl(jsKeyUrl);
        }

        String ocrApi = rule.optString("ocr_api", "");
        if (!ocrApi.isEmpty()) {
            verifier.setDdddOcrApi(ocrApi);
        }

        // 图形验证码 OCR 通道配置
        String ocrCaptchaUrl = rule.optString("ocr_captcha_url", "");
        if (!ocrCaptchaUrl.isEmpty()) {
            verifier.setOcrCaptchaUrl(ocrCaptchaUrl);
        }
        String ocrVerifyUrl = rule.optString("ocr_verify_url", "");
        if (!ocrVerifyUrl.isEmpty()) {
            verifier.setOcrVerifyUrl(ocrVerifyUrl);
        }
        String ocrCaptchaApi = rule.optString("ocr_captcha_api", "");
        if (!ocrCaptchaApi.isEmpty()) {
            verifier.setOcrCaptchaApi(ocrCaptchaApi);
        }

        return verifier;
    }

    private void configureSliderVerifier(SliderVerifyUtils verifier) {
        try {
            
            String verifyType = rule.optString("verify_type", "auto");
            if ("slider".equals(verifyType)) {
                verifier.setVerifyType(SliderVerifyUtils.VerifyType.SLIDER);
            } else if ("click".equals(verifyType)) {
                verifier.setVerifyType(SliderVerifyUtils.VerifyType.CLICK);
            } else if ("image".equals(verifyType)) {
                verifier.setVerifyType(SliderVerifyUtils.VerifyType.IMAGE_OCR);
            }
            
        } catch (Exception e) {
            SpiderDebug.log("验证器偏好配置异常: " + e.getMessage());
        }
    }

    private void mergeVerifyCookie(SliderVerifyUtils verifier) {
        try {
            String verifyCookie = verifier.getVerifyCookie();
            if (verifyCookie == null || verifyCookie.isEmpty()) return;

            if (!rule.has("header")) {
                rule.put("header", new JSONObject());
            }
            JSONObject hdr = headerObject();
            String existingCookie = hdr.optString("cookie", "");
            String merged = Util.mergeCookies(existingCookie, verifyCookie);
            hdr.put("cookie", merged);

            SpiderDebug.log("验证 Cookie 已合并到全局请求头");
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    protected void extractAllCookies(okhttp3.Response response) {
        try {
            List<String> cookies = response.headers("set-cookie");
            if (cookies.isEmpty()) return;

            StringBuilder merged = new StringBuilder();
            for (String cookie : cookies) {
                
                int semiIdx = cookie.indexOf(';');
                String nvPair = semiIdx > 0 ? cookie.substring(0, semiIdx) : cookie;
                if (merged.length() > 0) merged.append("; ");
                merged.append(nvPair.trim());
            }

            if (merged.length() > 0) {
                JSONObject hdr = headerObject();
                String existing = hdr.optString("cookie", "");
                String result = Util.mergeCookies(existing, merged.toString());
                hdr.put("cookie", result);
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    private JSONObject headerObject() throws JSONException {
        Object obj = rule.opt("header");
        if (obj instanceof JSONObject) return (JSONObject) obj;
        JSONObject hdr = parseHeader(obj == null ? "" : obj.toString());
        rule.put("header", hdr);
        return hdr;
    }

    private Map<String, String> mergeHeaders(Map<String, String> base, JSONObject extra) {
        if (extra == null) return base;
        Map<String, String> result = new HashMap<>(base);
        try {
            Iterator<String> iter = extra.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                result.put(key, extra.getString(key));
            }
        } catch (Exception e) {
            SpiderDebug.log("合并扩展请求头异常: " + e.getMessage());
        }
        return result;
    }

    private String fetchUrlWithRetry(String url) {
        try {
            Map<String, String> headers = getHeaders(url);
            
            long delay = ANTI_CRAWLER_DELAY_MS + (long)(Math.random() * 1000);
            Thread.sleep(delay);
            return OkHttp.string(url, headers);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private String appendQueryParam(String url, String key, String value) {
        try {
            URL u = new URL(url);
            String sep = u.getQuery() == null ? "?" : "&";
            return url + sep + key + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return url + (url.contains("?") ? "&" : "?") + key + "=" + value;
        }
    }

    protected String string2Hex(String str) {
        if (str == null || str.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
    
    protected String removeHtml(String text) {
        if (text == null || text.isEmpty()) return "";
        try {
            return Jsoup.parse(text).text();
        } catch (Exception e) {
            return text;
        }
    }
    
    protected Element selectByRule(Document doc, String rule) {
        if (doc == null || rule == null || rule.isEmpty()) return null;
        
        try {
            
            if (rule.contains("||")) {
                String[] parts = rule.split("\\|\\|");
                for (String part : parts) {
                    Element result = selectByRule(doc, part.trim());
                    if (result != null) return result;
                }
                return null;
            }
            
            if (rule.contains("--")) {
                String[] parts = rule.split("--");
                Element current = doc;
                for (String part : parts) {
                    if (current == null) return null;
                    current = current.selectFirst(part.trim());
                }
                return current;
            }
            
            if (rule.contains(":eq(")) {
                Pattern p = P_SELECT_EQ;
                Matcher m = p.matcher(rule);
                if (m.matches()) {
                    String selector = m.group(1);
                    int index = Integer.parseInt(m.group(2));
                    Elements elements = doc.select(selector);
                    return (index >= 0 && index < elements.size()) ? elements.get(index) : null;
                }
            }
            
            if (rule.contains(":gt(") || rule.contains(":lt(")) {
                return doc.selectFirst(rule);
            }
            
            return doc.selectFirst(rule);
        } catch (Exception e) {
            SpiderDebug.log("selectByRule 异常：" + e.getMessage());
            return null;
        }
    }
    
    protected String getTextByRule(Document doc, String rule) {
        if (doc == null || rule == null || rule.isEmpty()) return "";
        
        try {
            
            if (rule.contains("+") || rule.contains("＋")) {
                String[] parts = rule.split("[+＋]");
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    Element elem = selectByRule(doc, part.trim());
                    if (elem != null) {
                        sb.append(elem.text());
                    }
                }
                return sb.toString();
            }
            
            Element elem = selectByRule(doc, rule);
            return elem != null ? elem.text() : "";
        } catch (Exception e) {
            SpiderDebug.log("getTextByRule 异常：" + e.getMessage());
            return "";
        }
    }
    
    protected boolean isJsonMode() {
        try {
            if (rule.has("cat_mode")) {
                String mode = rule.getString("cat_mode");
                return "0".equals(mode);
            }
            
            return rule.optBoolean("json", false);
        } catch (Exception e) {
            return false;
        }
    }
    
    protected String applyTwiceCut(String content, String twicePre, String twiceSuf) {
        if (content == null || content.isEmpty()) return "";
        
        try {
            
            boolean needTwice = false;
            if (rule.has("cat_YN_twice")) {
                needTwice = "1".equals(rule.getString("cat_YN_twice"));
            } else if (rule.has("YN_twice")) {
                needTwice = rule.getBoolean("YN_twice");
            }
            
            if (!needTwice || twicePre == null || twicePre.isEmpty() || 
                twiceSuf == null || twiceSuf.isEmpty()) {
                return content;
            }
            
            int startIdx = content.indexOf(twicePre);
            if (startIdx == -1) return content;
            startIdx += twicePre.length();
            
            int endIdx = content.indexOf(twiceSuf, startIdx);
            if (endIdx == -1) return content;
            
            return content.substring(startIdx, endIdx);
        } catch (Exception e) {
            SpiderDebug.log("applyTwiceCut 异常：" + e.getMessage());
            return content;
        }
    }
    
    protected List<String> extractArray(String content, String arrPre, String arrSuf) {
        List<String> result = new ArrayList<>();
        if (content == null || content.isEmpty() || 
            arrPre == null || arrPre.isEmpty() || 
            arrSuf == null || arrSuf.isEmpty()) {
            return result;
        }
        
        try {
            int startPos = 0;
            while (result.size() < MAX_MATCH_COUNT) {
                int startIdx = content.indexOf(arrPre, startPos);
                if (startIdx == -1) break;
                startIdx += arrPre.length();

                int endIdx = content.indexOf(arrSuf, startIdx);
                if (endIdx == -1) break;

                result.add(content.substring(startIdx, endIdx));
                startPos = endIdx + arrSuf.length();
            }
        } catch (Exception e) {
            SpiderDebug.log("extractArray 异常：" + e.getMessage());
        }
        
        return result;
    }
    
    protected String fetchPostForm(String webUrl, Map<String, String> params, String charset) {
        if (charset == null || charset.isEmpty()) charset = "UTF-8";

        try {
            
            if (isInternalUrl(webUrl)) {
                SpiderDebug.log(safeLog("fetchPostForm SSRF blocked: " + webUrl));
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), charset))
                  .append("=")
                  .append(URLEncoder.encode(entry.getValue(), charset));
            }
            
            Map<String, String> headers = getHeaders(webUrl);
            headers.put("Content-Type", "application/x-www-form-urlencoded; charset=" + charset);
            
            String response = OkHttp.post(webUrl, sb.toString(), headers);
            return response != null ? response : "";
        } catch (Exception e) {
            SpiderDebug.log("fetchPostForm 异常：" + e.getMessage());
            return "";
        }
    }
    
    protected Pair<String, Map<String, List<String>>> fetchWithHeaders(String webUrl, HashMap<String, String> headers) {
        okhttp3.Response response = null;
        try {
            response = OkHttp.newCall(webUrl, headers);
            String body = response.body() != null ? response.body().string() : "";

            Map<String, List<String>> responseHeaders = new HashMap<>();
            for (String name : response.headers().names()) {
                responseHeaders.put(name, response.headers(name));
            }

            return new Pair<>(body, responseHeaders);
        } catch (Exception e) {
            SpiderDebug.log("fetchWithHeaders 异常：" + e.getMessage());
            return new Pair<>("", new HashMap<>());
        } finally {
            if (response != null) response.close();
        }
    }
    
    protected List<String> buildYearRange(int startYear, int endYear) {
        List<String> years = new ArrayList<>();
        
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        
        if (endYear <= 0) endYear = currentYear;
        if (startYear <= 0) startYear = endYear - 10;
        
        for (int year = startYear; year <= endYear; year++) {
            years.add(String.valueOf(year));
        }
        
        return years;
    }
    
    protected JSONObject loadExtFilter(String url) {
        try {
            if (url.startsWith("clan://")) {
                
                String filePath = url.substring(7);
                String content = Util.readStringFromFile(filePath);
                return new JSONObject(content);
            } else if (url.startsWith("http")) {
                
                String content = OkHttp.string(url, null);
                return new JSONObject(content);
            }
        } catch (Exception e) {
            SpiderDebug.log("loadExtFilter 异常：" + e.getMessage());
        }
        return new JSONObject();
    }

    protected String fixCover(String cover, String site) {
        try {
            if (cover == null || cover.isEmpty()) {
                
                String fallback = getRuleVal("default_pic").trim();
                if (fallback.isEmpty()) return cover;
                log("fixCover 空封面回退默认图: " + fallback);
                return addHttpPrefix(fallback);
            }
            log("fixCover site=" + site + " cover=" + cover);

            if (!baseEncodeUrl.isEmpty()) {
                String encoded = Base64.encodeToString(cover.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                return baseEncodeUrl + encoded;
            }

            StringBuilder sb = new StringBuilder("proxy://do=XBPQ")
                    .append("&url=").append(URLEncoder.encode(cover, "UTF-8"))
                    .append("&referer=").append(URLEncoder.encode(site, "UTF-8"));
            
            if (!secretKey.isEmpty()) {
                sb.append("&key=").append(Util.md5(cover + secretKey));
            }
            return sb.toString();
        } catch (Exception e) {
            SpiderDebug.log("fixCover error: " + e.getMessage());
        }
        return cover;
    }

    private static volatile Map<String, String> picHeaderCache = null;

    private static boolean isInternalUrl(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase();
        if (u.isEmpty()) return false;
        if (u.startsWith("file:") || u.startsWith("gopher:") || u.startsWith("dict:")
                || u.startsWith("ftp:") || u.startsWith("jar:") || u.startsWith("netdoc:")) {
            return true;
        }
        String host = "";
        boolean isIpv6 = false;
        int idx = u.indexOf("://");
        if (idx >= 0) {
            int start = idx + 3;
            if (start < u.length() && u.charAt(start) == '[') {
                int close = u.indexOf(']', start);
                host = close > 0 ? u.substring(start + 1, close) : "";
                isIpv6 = true;
            } else {
                int end = u.length();
                for (int i = start; i < u.length(); i++) {
                    char c = u.charAt(i);
                    if (c == '/' || c == ':' || c == '?' || c == '#') { end = i; break; }
                }
                host = u.substring(start, end);
            }
        } else {
            int cut = u.indexOf('/');
            host = cut >= 0 ? u.substring(0, cut) : u;
        }
        if (host.isEmpty()) return false;
        if (!isIpv6) {
            int port = -1;
            int colon = host.indexOf(':');
            if (colon > 0) {
                String portStr = host.substring(colon + 1);
                host = host.substring(0, colon);
                try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) { return true; }
            }
            if (port > 0 && port != 80 && port != 443) {
                return true;
            }
            return isInternalHost(host);
        }
        
        if (host.equals("::1") || host.equals("::")) return true;
        if (host.startsWith("fc") || host.startsWith("fd")) return true; 
        if (host.startsWith("fe80")) return true;                        
        
        if (host.startsWith("::ffff:")) return isInternalHost(host.substring(7));
        return resolvesToInternal(host);
    }

    private static boolean isInternalHost(String host) {
        if (host.isEmpty()) return false;
        if (host.equals("localhost")) return true;
        if (isInternalIpv4(host)) return true;
        if (host.endsWith(".local") || host.endsWith(".internal") || host.endsWith(".localhost")) {
            return true;
        }
        if (looksLikeEncodedIp(host)) {
            // 整数形式 IP（如 2130706433 = 127.0.0.1）直接判内网，防绕过
            if (host.matches("\\d{8,}")) return true;
            if (resolvesToInternal(host)) return true;
        }
        return false;
    }

    private static boolean isInternalIpv4(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) return false;
        try {
            long a = Long.parseLong(parts[0]);
            long b = Long.parseLong(parts[1]);
            long c = Long.parseLong(parts[2]);
            long d = Long.parseLong(parts[3]);
            if (a > 255 || b > 255 || c > 255 || d > 255) return false;
            // 前导零(潜在八进制绕过，如 0177.0.0.1) 一律按内网处理，防绕过
            for (String p : parts) {
                if (p.length() > 1 && p.charAt(0) == '0') return true;
            }
            if (a == 10) return true;                        
            if (a == 172 && b >= 16 && b <= 31) return true; 
            if (a == 192 && b == 168) return true;           
            if (a == 127) return true;                       
            if (a == 169 && b == 254) return true;           
            if (a == 0) return true;                         
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean looksLikeEncodedIp(String host) {
        if (host.isEmpty()) return false;
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == '.' || c == 'x') continue;
            return false;
        }
        return true;
    }

    private static boolean resolvesToInternal(String host) {
        try {
            java.net.InetAddress[] addrs = java.net.InetAddress.getAllByName(host);
            for (java.net.InetAddress addr : addrs) {
                if (addr.isLoopbackAddress() || addr.isAnyLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                    return true;
                }
                byte[] b = addr.getAddress();
                if (b.length == 4 && (b[0] & 0xFF) == 100
                        && (b[1] & 0xFF) >= 64 && (b[1] & 0xFF) <= 127) {
                    return true; 
                }
            }
        } catch (Exception e) {
            SpiderDebug.log(safeLog("DNS resolve failed, fail-closed: " + host));
            return true;
        }
        return false;
    }
    private static String safeLog(String s) {

        if (s == null) return "";
        
        String r = s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", " ").replace("\r", " ");
        if (r.length() > 2000) r = r.substring(0, 2000) + "...";
        return r;
    }

    public static Object[] loadPic(Map<String, String> params) {
        XBPQ inst = activeInstance;
        return inst == null ? null : inst.doLoadPic(params);
    }

    private Object[] doLoadPic(Map<String, String> params) {
        try {

            String pic = params.containsKey("url") ? params.get("url") : params.get("pic");
            String site = params.containsKey("referer") ? params.get("referer") : params.get("site");
            if (pic == null || pic.isEmpty()) return null;

            pic = java.net.URLDecoder.decode(pic, "UTF-8");

            if (isInternalUrl(pic)) {
                SpiderDebug.log(safeLog("loadPic SSRF blocked: " + pic));
                return null;
            }

            if (!isProxyOriginAllowed(pic)) {
                SpiderDebug.log(safeLog("loadPic origin not in allowlist: " + pic));
                return null;
            }

            if (!secretKey.isEmpty()) {
                String reqKey = params.get("key");
                String expectKey = Util.md5(pic + secretKey);
                if (reqKey == null || !reqKey.equals(expectKey)) {
                    SpiderDebug.log("loadPic key mismatch, reject proxy fetch");
                    return null;
                }
            }

            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", UA_PC);
            if (site != null && !site.isEmpty()) {
                headers.put("referer", site);
            }

            Object[] result = OkHttp.proxy(pic, headers);
            if (result != null && ((Integer) result[0]) == 200) {
                ByteArrayInputStream stream = new ByteArrayInputStream((byte[]) result[2]);
                Object[] proxyResult = new Object[3];
                proxyResult[0] = 200;
                proxyResult[1] = result[1];
                proxyResult[2] = stream;
                return proxyResult;
            }
        } catch (Throwable th) {
            SpiderDebug.log(th);
        }
        return null;
    }

    public static Object[] loadDanmu(Map<String, String> map) {
        XBPQ inst = activeInstance;
        return inst == null ? null : inst.doLoadDanmu(map);
    }

    private Object[] doLoadDanmu(Map<String, String> map) {
        try {
            String danmuUrl = map.get("danmu_url");
            if (danmuUrl == null || danmuUrl.isEmpty()) {
                danmuUrl = map.get("danmuUrl");
            }
            if (danmuUrl == null || danmuUrl.isEmpty()) return null;
            danmuUrl = java.net.URLDecoder.decode(danmuUrl, "UTF-8");

            if (isInternalUrl(danmuUrl)) {
                SpiderDebug.log(safeLog("loadDanmu SSRF blocked: " + danmuUrl));
                return null;
            }

            if (!isProxyOriginAllowed(danmuUrl)) {
                SpiderDebug.log(safeLog("loadDanmu origin not in allowlist: " + danmuUrl));
                return null;
            }

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("User-Agent", UA_PC);
            
            if (!headerMap.isEmpty()) headers.putAll(headerMap);

            String resp = OkHttp.string(danmuUrl, headers);
            if (resp == null || resp.isEmpty()) return null;
            String xml = jsonArray2xml(resp);
            if (xml.isEmpty()) xml = resp;
            return new Object[]{200, "application/octet-stream",
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))};
        } catch (Exception e) {
            SpiderDebug.log(safeLog("loadDanmu error: " + e.getMessage()));
            return null;
        }
    }

    private static final int DM_TIME = 0, DM_MODE = 1, DM_SIZE = 2, DM_COLOR = 3,
            DM_SOURCE = 4, DM_CONTENT = 5, DM_USER = 6;

    private static String escapeXml(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static String jsonArray2xml(String input) {
        if (input == null || input.isEmpty()) return "";
        try {
            JSONArray array = new JSONObject(input).optJSONArray("list");
            if (array == null) return "";
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><i>\n");
            for (int i = 0; i < array.length(); i++) {
                
                try {
                    JSONArray item = array.getJSONArray(i);
                    if (item.length() < 7) continue;
                    sb.append("<d p=\"")
                            .append(item.optString(DM_TIME))
                            .append(",").append(item.optInt(DM_MODE))
                            .append(",").append(item.optInt(DM_SIZE))
                            .append(",").append(item.optInt(DM_COLOR))
                            .append(",").append(item.optInt(DM_SOURCE))
                            .append(",0,").append(item.optString(DM_USER))
                            .append("\">").append(escapeXml(item.optString(DM_CONTENT))).append("</d>\n");
                } catch (Exception rowEx) {
                    SpiderDebug.log("jsonArray2xml 跳过第 " + i + " 条: " + rowEx.getMessage());
                }
            }
            sb.append("</i>");
            return sb.toString();
        } catch (Exception e) {
            SpiderDebug.log("jsonArray2xml error: " + e.getMessage());
            return "";
        }
    }

    public static Object[] loadM3u8(Map<String, String> map) {
        XBPQ inst = activeInstance;
        return inst == null ? null : inst.doLoadM3u8(map);
    }

    private Object[] doLoadM3u8(Map<String, String> map) {
        try {
            String m3u8Url = map.get("url");
            String baseUrl = map.get("base");
            if (m3u8Url == null || m3u8Url.isEmpty()) return null;
            m3u8Url = java.net.URLDecoder.decode(m3u8Url, "UTF-8");

            boolean isBase64Content = false;
            if (m3u8Url.indexOf("://") < 0 && m3u8Url.length() > 200) {
                try {
                    byte[] decoded = Base64.decode(m3u8Url, Base64.DEFAULT);
                    String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                    if (decodedStr.startsWith("#EXTM3U") || decodedStr.contains("#EXTINF")) {
                        isBase64Content = true;
                        m3u8Url = decodedStr;
                    }
                } catch (Exception ignored) {
                }
            }

            if (!isBase64Content && isInternalUrl(m3u8Url)) {
                SpiderDebug.log(safeLog("loadM3u8 SSRF blocked: " + m3u8Url));
                return null;
            }

            if (!isBase64Content && !isProxyOriginAllowed(m3u8Url)) {
                SpiderDebug.log(safeLog("loadM3u8 origin not in allowlist: " + m3u8Url));
                return null;
            }

            String content;
            if (isBase64Content) {
                content = m3u8Url;
            } else {
                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("User-Agent", UA_PC);
                if (!headerMap.isEmpty()) headers.putAll(headerMap);
                content = OkHttp.string(m3u8Url, headers);
            }
            if (content == null || content.isEmpty()) return null;

            StringBuilder result = new StringBuilder();
            String[] lines = content.split("\n");
            String resolvedBase = baseUrl != null && !baseUrl.isEmpty()
                    ? baseUrl
                    : (m3u8Url.startsWith("https") ? "https://" : "http://") + Util.extractDomain(m3u8Url) + "/";

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    result.append(line).append("\n");
                } else if (!line.startsWith("http") && !line.startsWith("//")) {
                    result.append(Util.repairUrl(resolvedBase, line)).append("\n");
                } else {
                    result.append(line).append("\n");
                }
            }

            String finalContent = result.toString();
            return new Object[]{200, "application/vnd.apple.mpegurl",
                    new ByteArrayInputStream(finalContent.getBytes(StandardCharsets.UTF_8))};
        } catch (Exception e) {
            SpiderDebug.log(safeLog("loadM3u8 error: " + e.getMessage()));
            return null;
        }
    }

    @Override
    public Object[] proxy(Map<String, String> params) {
        if (params == null) return null;
        try {
            
            fetchRule();
            initEnhancedConfig();
        } catch (Exception ignored) {
        }
        if (params.containsKey("danmu_url") || params.containsKey("danmuUrl")) return loadDanmu(params);
        if (params.containsKey("m3u8")) return loadM3u8(params);
        
        String pu = params.get("url");
        if (pu != null) {
            try {
                String decoded = java.net.URLDecoder.decode(pu, "UTF-8");
                if (decoded.toLowerCase().contains(".m3u8")) return loadM3u8(params);
                // base64 形态的 m3u8 内容无 .m3u8 标记，解码探测后同样走 m3u8 分支
                if (decoded.length() > 200 && !decoded.contains("://")) {
                    String inner = new String(Base64.decode(decoded, Base64.DEFAULT), StandardCharsets.UTF_8);
                    if (inner.contains("#EXTM3U") || inner.contains("#EXTINF")) return loadM3u8(params);
                }
            } catch (Exception ignored) {
            }
        }
        return loadPic(params);
    }

    private void initEnhancedConfig() {
        try {
            
            if (rule.has("openDebug")) {
                isDebug = "1".equals(rule.optString("openDebug")) || rule.optBoolean("openDebug", false);
            }
            playImage = getRuleVal("play_image");
            mergeLines = "1".equals(getRuleVal("merge_lines"));
            hotRecommend = "1".equals(getRuleVal("hot_recommend"));
            listDisplay = "1".equals(getRuleVal("list_display"));

            variableMap.clear();
            variableMap.put("主页url", rule.optString("homeUrl", ""));
            variableMap.put("站名", rule.optString("siteName", ""));
            variableMap.put("作者", rule.optString("author", ""));
            variableMap.put("分类url", rule.optString("class_url", ""));
            variableMap.put("搜索url", rule.optString("search_url", ""));
            variableMap.put("后缀", rule.optString("domainSuffix", ""));
            variableMap.put("密钥", getRuleVal("secretKey"));

            applyDynamicDomain();

            String webViewCfg = getRuleVal("web_view");
            if (!webViewCfg.isEmpty() && !"0".equals(webViewCfg)) {
                SpiderDebug.log("「WebView渲染」(web_view) 当前版本未实现，已忽略该配置；"
                        + "如站点需执行 JS 请改用 jump_url 或 force_play");
            }

            baseEncodeUrl = getRuleVal("baseEncodeUrl");
            secretKey = getRuleVal("secretKey");
            staticHomeUrl = rule.optString("homeUrl", "");
            initProxyAllowlist();

            String headerJsonStr = getRuleVal("headerJson");
            String userHeaderStr = getRuleVal("userHeader");
            if (!headerJsonStr.isEmpty() || !userHeaderStr.isEmpty()) {
                headerMap.clear();
                if (!headerJsonStr.isEmpty()) {
                    try {
                        JSONObject headerObj = headerJsonStr.startsWith("{")
                                ? new JSONObject(headerJsonStr)
                                : parseHeader(headerJsonStr);
                        Iterator<String> keys = headerObj.keys();
                        while (keys.hasNext()) {
                            String k = keys.next();
                            headerMap.put(k, headerObj.optString(k));
                        }
                    } catch (Exception e) {
                        SpiderDebug.log("headerMap parse error: " + e.getMessage());
                    }
                }

                if (!userHeaderStr.isEmpty()) {
                    injectUserHeader(userHeaderStr);
                }
            }
        } catch (Exception e) {
            SpiderDebug.log("initEnhancedConfig error: " + e.getMessage());
        }
    }

    private void initProxyAllowlist() {
        proxyAllowedOrigins.clear();
        String allowRaw = getRuleVal("pic_allow_domains");
        if (!allowRaw.isEmpty()) {
            for (String part : allowRaw.split("[,，;；\\n]")) {
                String item = part.trim();
                if (item.isEmpty()) continue;
                String origin = originOf(item);
                proxyAllowedOrigins.add(origin.isEmpty() ? item.toLowerCase() : origin);
            }
        }
        String homeOrigin = originOf(rule.optString("homeUrl", ""));
        if (!homeOrigin.isEmpty()) proxyAllowedOrigins.add(homeOrigin);
        String dyn = getRuleVal("dynamic_domain").trim();
        String dynOrigin = originOf(dyn);
        if (!dynOrigin.isEmpty()) proxyAllowedOrigins.add(dynOrigin);
        String fallback = getRuleVal("home_url_c").trim();
        String fallbackOrigin = originOf(fallback);
        if (!fallbackOrigin.isEmpty()) proxyAllowedOrigins.add(fallbackOrigin);
    }

    private boolean isProxyOriginAllowed(String url) {
        if (proxyAllowedOrigins.isEmpty()) return true;
        String origin = originOf(url);
        if (origin.isEmpty()) return false;
        if (proxyAllowedOrigins.contains(origin)) return true;
        try {
            String host = new URL(origin).getHost();
            if (host != null) {
                for (String allow : proxyAllowedOrigins) {
                    if (allow.equalsIgnoreCase(host) || host.toLowerCase().endsWith("." + allow.toLowerCase())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void applyDynamicDomain() {
        String fallbackRaw = getRuleVal("home_url_c");
        String dynRaw = getRuleVal("dynamic_domain");
        String fallback = applyPostProcessors(resolveVariables(fallbackRaw)).trim();
        String dyn = applyPostProcessors(resolveVariables(dynRaw)).trim();
        String home = rule.optString("homeUrl", "");
        if (!isValidOrigin(fallback)) fallback = fallbackRaw.trim();
        variableMap.put("域名-c", isValidOrigin(dyn) ? dyn : home);
        variableMap.put("主页url-c", isValidOrigin(fallback) ? fallback : home);

        if (isValidOrigin(dyn)) {
            String oldOrigin = originOf(home);
            String newOrigin = originOf(dyn);
            if (!oldOrigin.isEmpty() && !newOrigin.isEmpty()
                    && !oldOrigin.equalsIgnoreCase(newOrigin)) {
                try {
                    String json = rule.toString();
                    // toString 会把 / 转义为 \/，需同时替换转义形式，否则动态域名切换永远落空
                    String escOld = oldOrigin.replace("/", "\\/");
                    String escNew = newOrigin.replace("/", "\\/");
                    String replaced = json.replace(oldOrigin, newOrigin).replace(escOld, escNew);
                    if (!replaced.equals(json)) {
                        rule = new JSONObject(replaced);
                        SpiderDebug.log(safeLog("动态域名已切换: " + oldOrigin + " -> " + newOrigin));
                    }
                } catch (Exception e) {
                    SpiderDebug.log(safeLog("动态域名切换失败: " + e.getMessage()));
                }
            }
        }
    }

    private static String originOf(String url) {
        if (url == null) return "";
        String u = url.trim();
        int sep = u.indexOf(";;");
        if (sep >= 0) u = u.substring(0, sep);
        if (!u.startsWith("http")) return "";
        try {
            java.net.URL parsed = new java.net.URL(u);
            if (parsed.getHost() == null || parsed.getHost().isEmpty()) return "";
            int port = parsed.getPort();
            return parsed.getProtocol() + "://" + parsed.getHost() + (port > 0 ? ":" + port : "");
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean isValidOrigin(String url) {
        return !originOf(url).isEmpty();
    }

    private void injectUserHeader(String raw) {
        if (raw == null || raw.isEmpty()) return;
        for (String part : raw.split("#")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            int idx = part.indexOf(':');
            if (idx <= 0) continue;
            String key = part.substring(0, idx).trim();
            String val = part.substring(idx + 1).trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                headerMap.put(key, val);
                
                SpiderDebug.log("注入 User 请求头: " + key + " -> " + maskHeaderValue(val));
            }
        }
    }

    private static String maskHeaderValue(String val) {
        if (val == null || val.isEmpty()) return "";
        if (val.length() <= 8) return "******";
        return val.substring(0, 4) + "****" + val.substring(val.length() - 2);
    }

    public static void log(String message) {
        XBPQ inst = activeInstance;
        if (inst == null || !inst.isDebug) return;
        SpiderDebug.log("XBPQ[debug]: " + message);
    }

    protected String resolveVariables(String template) {
        if (template == null || template.isEmpty()) return template;
        if (!template.contains("{{")) return template;
        try {
            
            if (rule != null) {
                variableMap.put("主页url", rule.optString("homeUrl", ""));
            }
            Pattern pattern = P_TEMPLATE_VAR;
            for (int iter = 0; iter < 10; iter++) {
                Matcher matcher = pattern.matcher(template);
                if (!matcher.find()) break;
                StringBuffer sb = new StringBuffer();
                matcher.reset();
                while (matcher.find()) {
                    String key = matcher.group(1).trim();
                    String val = variableMap.getOrDefault(key, "");
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(val));
                }
                matcher.appendTail(sb);
                template = sb.toString();
            }
            return template;
        } catch (Exception e) {
            SpiderDebug.log("resolveVariables error: " + e.getMessage());
            return template;
        }
    }

    protected static String hexEscapeDecode(String input) {
        if (input == null || input.isEmpty()) return input;
        try {
            Pattern pattern = P_UNICODE_ESCAPE;
            Matcher matcher = pattern.matcher(input);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String hex = matcher.group(1);
                char c = (char) Integer.parseInt(hex, 16);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(c)));
            }
            matcher.appendTail(sb);
            String result = sb.toString();
            
            result = result.replace("\r\n", "");
            
            result = result.replaceAll("\\\\\\+(?!\")", "");
            return result.trim();
        } catch (Exception e) {
            SpiderDebug.log("hexEscapeDecode error: " + e.getMessage());
            return input;
        }
    }

    public boolean isFail(int code) {
        try {
            JSONObject r = rule;
            if (r != null) {
                String codeStr = String.valueOf(code);
                String failCodes = r.optString("failCodes", "") + "#" + r.optString("errorCodes", "");
                for (String fc : failCodes.split("[#,，]")) {
                    if (!fc.trim().isEmpty() && codeStr.equals(fc.trim())) return true;
                }
            }
        } catch (Exception ignored) {
        }
        return code >= 400;
    }

    public boolean isSuccess(int code) {
        try {
            JSONObject r = rule;
            if (r != null) {
                String successCodes = r.optString("successCodes", "");
                if (!successCodes.isEmpty()) {
                    String codeStr = String.valueOf(code);
                    for (String sc : successCodes.split("[#,，]")) {
                        if (codeStr.equals(sc.trim())) return true;
                    }
                    return false;
                }
            }
        } catch (Exception ignored) {
        }
        return code >= 200 && code < 300;
    }

    private static final String PREF_MENU_KEY = "XBPQ_prefMenu";

    private static String readPref(String key) {
        try {
            return Init.getString(key, "");
        } catch (Exception e) {
            return "";
        }
    }

    private String randomColor() {
        return String.valueOf(random.nextInt(100));
    }

    private JSONArray buildPrefMenu() {
        JSONArray menu = new JSONArray();
        try {
            
            JSONArray custom = rule == null ? null : rule.optJSONArray("prefMenu");
            if (custom != null && custom.length() > 0) return custom;

            menu.put(new JSONObject().put("name", "置顶搜索和设置").put("action", "SSTop").put("selected", false));
            menu.put(new JSONObject().put("name", "显示收藏夹").put("action", "favoritesShow").put("selected", false));
            menu.put(new JSONObject().put("name", "关闭搜索记录").put("action", "offSearchCache").put("selected", false));
            menu.put(new JSONObject().put("name", "打开调试模式").put("action", "openDebug").put("selected", isDebug));
        } catch (Exception ignored) {
        }
        return menu;
    }

    private JSONArray getPrefMenu() {
        try {
            String saved = readPref(PREF_MENU_KEY);
            JSONArray savedArr = null;
            if (saved != null && saved.length() > 0) {
                try {
                    savedArr = new JSONArray(saved);
                } catch (Exception ignored) {
                }
            }
            JSONArray defaults = buildPrefMenu();
            if (savedArr == null) return defaults;
            JSONArray merged = new JSONArray();
            for (int i = 0; i < defaults.length(); i++) {
                JSONObject item = defaults.getJSONObject(i);
                for (int j = 0; j < savedArr.length(); j++) {
                    JSONObject sv = savedArr.getJSONObject(j);
                    if (item.optString("action").equals(sv.optString("action"))) {
                        item.put("selected", sv.optBoolean("selected"));
                        break;
                    }
                }
                merged.put(item);
            }
            return merged;
        } catch (Exception e) {
            return buildPrefMenu();
        }
    }

    private void applyPrefMenu() {
        try {
            if (rule == null) return;
            JSONArray menu = getPrefMenu();
            for (int i = 0; i < menu.length(); i++) {
                JSONObject item = menu.getJSONObject(i);
                rule.put(item.optString("action"), item.optBoolean("selected"));
            }
        } catch (Exception ignored) {
        }
    }

    protected JSONArray insertActionTabs(JSONArray classes) {
        try {
            if (rule == null) return classes;
            boolean top = rule.optBoolean("SSTop", false);
            if (!"1".equals(getRuleVal("actionTabs")) && !top) return classes;

            JSONObject setAct = new JSONObject();
            setAct.put("actionId", "偏好设置");
            setAct.put("title", "偏好设置");
            setAct.put("type", 2);
            setAct.put("width", 800);
            setAct.put("option", getPrefMenu());
            JSONObject setTab = new JSONObject();
            setTab.put("vod_name", "偏好设置");
            setTab.put("vod_id", "偏好设置");
            setTab.put("vod_pic", "clan://assets/set.png?bgcolor=" + randomColor());
            setTab.put("vod_tag", "action");
            setTab.put("action", setAct);

            JSONObject input = new JSONObject();
            input.put("id", "wd");
            input.put("tip", "请输入搜索内容");
            input.put("value", "");
            JSONObject searchAct = new JSONObject();
            searchAct.put("actionId", "源内搜索");
            searchAct.put("title", "源内搜索");
            searchAct.put("type", 1);
            searchAct.put("input", new JSONArray().put(input));
            JSONObject searchTab = new JSONObject();
            searchTab.put("vod_name", "源内搜索");
            searchTab.put("vod_id", "源内搜索");
            searchTab.put("vod_pic", "clan://assets/search.png?bgcolor=" + randomColor());
            searchTab.put("vod_tag", "action");
            searchTab.put("action", searchAct);

            JSONArray result = new JSONArray();
            if (top) {
                result.put(searchTab);
                result.put(setTab);
                for (int i = 0; i < classes.length(); i++) result.put(classes.get(i));
            } else {
                for (int i = 0; i < classes.length(); i++) result.put(classes.get(i));
                result.put(searchTab);
                result.put(setTab);
            }
            return result;
        } catch (Exception e) {
            SpiderDebug.log("insertActionTabs error: " + e.getMessage());
            return classes;
        }
    }

    public static String clan(String input) {
        if (input == null || input.isEmpty()) return input;
        
        if (input.length() > 4096) input = input.substring(0, 4096);
        String s = P_CLAN_BRACKET.matcher(input).replaceAll("");
        s = P_CLAN_YUAN.matcher(s).replaceAll("");
        return s;
    }

    public static String getBL(String path) {
        XBPQ inst = activeInstance;
        return inst == null ? "" : inst.doGetBL(path);
    }

    private String doGetBL(String path) {
        try {
            if (staticHomeUrl.isEmpty()) return "";
            String fullUrl = path.startsWith("http") ? path : Util.repairUrl(staticHomeUrl, path);
            if (isInternalUrl(fullUrl)) {
                SpiderDebug.log(safeLog("getBL SSRF blocked: " + fullUrl));
                return "";
            }
            Map<String, String> headers = new HashMap<>();
            if (!headerMap.isEmpty()) headers.putAll(headerMap);
            headers.put("User-Agent", UA_PC);
            return OkHttp.string(fullUrl, headers);
        } catch (Exception e) {
            SpiderDebug.log("getBL error: " + e.getMessage());
            return "";
        }
    }

    public static String getCom() {
        XBPQ inst = activeInstance;
        return inst == null ? "" : inst.staticHomeUrl;
    }

    public static void setBL(String path, String content) {
        XBPQ inst = activeInstance;
        if (inst != null) {
            inst.staticHomeUrl = (content != null && !content.isEmpty()) ? content : (path != null ? path : "");
        }
    }

    public static String getRV(String selector) {
        XBPQ inst = activeInstance;
        return inst == null ? "" : inst.doGetRV(selector);
    }

    private String doGetRV(String selector) {
        try {
            if (staticHomeUrl.isEmpty() || selector == null || selector.isEmpty()) return "";
            String html = doGetBL(staticHomeUrl);
            if (html == null || html.isEmpty()) return "";
            if (JsoupExtractor.isCssRule(selector)) {
                return JsoupExtractor.extractSingle(html, selector, null);
            }
            if (selector.contains("&&")) {
                String[] se = selector.split("&&", 2);
                List<String> r = subContent(html, se[0], se[1]);
                return r.isEmpty() ? "" : cleanHtml(r.get(0));
            }
            return JsoupExtractor.extractSingle(html, "css:" + selector, null);
        } catch (Exception e) {
            SpiderDebug.log("getRV error: " + e.getMessage());
            return "";
        }
    }

    public static String parseCssShortSyntax(String selector) {
        if (selector == null || !selector.startsWith("p:") || !selector.contains("->")) return selector;
        try {
            String cssExpr = selector.substring(2); 
            String[] parts = cssExpr.split("->");
            String tagPart = parts[0].trim();
            String attrPart = parts.length > 1 ? parts[1].trim() : "";
            if (attrPart.isEmpty() || "text".equals(attrPart)) {
                return tagPart;
            }
            return tagPart + "@" + attrPart;
        } catch (Exception e) {
            return selector;
        }
    }

    private static Context appContext = null;

    public static Context getContext() {
        if (appContext == null) return null;
        return appContext.getApplicationContext();
    }

    public static XBPQ getXbpq() {
        return instanceRef;
    }

    private static final XBPQ instanceRef = new XBPQ();

    public String decrypt(String src, String key, String iv, String mode) throws Exception {
        try {
            javax.crypto.spec.SecretKeySpec keySpec =
                    new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"), "AES");
            javax.crypto.Cipher cipher =
                    javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec,
                    new javax.crypto.spec.IvParameterSpec(iv.getBytes("UTF-8")));
            byte[] decrypted = cipher.doFinal(android.util.Base64.decode(src, android.util.Base64.DEFAULT));
            return new String(decrypted, mode == null || mode.isEmpty() ? "UTF-8" : mode);
        } catch (Exception e) {
            SpiderDebug.log(e);
            return "";
        }
    }

    public String encrypt(String data, String key, String iv, String mode) throws Exception {
        try {
            javax.crypto.spec.SecretKeySpec keySpec =
                    new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"), "AES");
            javax.crypto.Cipher cipher =
                    javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec,
                    new javax.crypto.spec.IvParameterSpec(iv.getBytes("UTF-8")));
            byte[] encrypted = cipher.doFinal(data.getBytes(mode == null || mode.isEmpty() ? "UTF-8" : mode));
            return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            SpiderDebug.log(e);
            return "";
        }
    }

    public String getToken(String src, String key, String iv, String mode) throws Exception {
        return encrypt(src, key, iv, mode);
    }

    /**
     * 四则运算求值（递归下降）。运算符优先级：() > 一元 +/- > * / % > + -，同级左结合。不支持函数调用。
     */
    public double mathEval(String expr) throws Exception {
        if (expr == null) return 0d;
        expr = expr.replace("（", "(").replace("）", ")")
                   .replace("×", "*").replace("÷", "/")
                   .replace("＋", "+").replace("－", "-")
                   .replace(",", "").replace(" ", "");
        return evalExpr(expr);
    }

    private static double evalExpr(String s) {
        return new Object() {
            int pos = -1, ch;

            // next() 必须同步更新 ch：否则 parse() 初始化后 ch 停留在初始值，
            // 以 '('、'-' 等开头的表达式会在数字分支读到空串而抛 NumberFormatException
            boolean next() {
                pos++;
                if (pos < s.length()) { ch = s.charAt(pos); return true; }
                pos = s.length();
                ch = '\0';
                return false;
            }

            boolean eat(int c) {
                if (pos < 0) { ch = s.charAt(0); pos = 0; }
                while (ch == ' ') if (!next()) return false;
                if (ch == c) { if (!next()) { pos = s.length(); } ch = pos < s.length() ? s.charAt(pos) : '\0'; return true; }
                return false;
            }

            double parse() {
                double v = parseTerm();
                while (true) {
                    if (eat('+')) v += parseTerm();
                    else if (eat('-')) v -= parseTerm();
                    else return v;
                }
            }

            /** 入口：先读入首个字符（ch 始终指向未消费的当前字符），parse 递归时不得再次前进 */
            double parseAll() {
                if (pos < 0) next();
                return parse();
            }

            double parseTerm() {
                double v = parseFactor();
                while (true) {
                    if (eat('*')) v *= parseFactor();
                    else if (eat('/')) v /= parseFactor();
                    else if (eat('%')) v %= parseFactor();
                    else return v;
                }
            }

            double parseFactor() {
                double v;
                boolean neg = false;
                if (eat('+')) return parseFactor();
                if (eat('-')) { neg = true; }
                if (eat('(')) {
                    v = parse();
                    eat(')');
                } else {
                    int start = pos;
                    while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) next();
                    v = Double.parseDouble(s.substring(start, pos));
                    ch = pos < s.length() ? s.charAt(pos) : '\0';
                }
                return neg ? -v : v;
            }
        }.parseAll();
    }

    public List<String> sortList(List<String> list) throws Exception {
        int size = list == null ? 0 : list.size();
        if (size < 2) return list;
        list.sort((a, b) -> {
            int va = numericWeight(a), vb = numericWeight(b);
            if (va != 0 && vb != 0) return Integer.compare(va, vb);
            if (va == 0 && vb != 0) return 1;
            if (va != 0 && vb == 0) return -1;
            return 0;
        });
        return list;
    }

    private static int numericWeight(String s) {
        if (s == null) return 0;
        String head = s.split("\\$")[0];
        Matcher m = P_EPISODE_NUM.matcher(head);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public boolean renderSniff(String url, String text) {
        if (text == null || text.length() < 1 || url == null) return false;
        String excludeList = "";
        try {
            if (rule != null) excludeList = rule.optString("嗅探排除", "");
        } catch (Exception e) {
            excludeList = "";
        }
        String[] parts = text.split("\\|");
        for (String part : parts) {
            if (part.length() < 1) continue;
            boolean matched;
            if ("视频链接".equals(part)) {
                matched = isVideoFormat(url);
            } else {
                matched = url.indexOf(part) >= 0;
            }
            if (!matched) continue;
            if (excludeList.length() > 0) {
                boolean excluded = false;
                for (String exclude : excludeList.split("\\|")) {
                    if (exclude.length() < 1) continue;
                    if (url.indexOf(exclude) >= 0) { excluded = true; break; }
                }
                if (excluded) continue;
            }
            return true;
        }
        return false;
    }

    private static final List<String> P2P_PROTOCOL_PREFIXES = Arrays.asList(
            "magnet:", "thunder:", "ed2k:", "qqdl:", "btih:", "urn:btih:");

    public boolean isP2pLink(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        for (String p : P2P_PROTOCOL_PREFIXES) {
            if (lower.startsWith(p)) return true;
        }
        
        if (lower.contains("xt=urn:btih:")) return true;
        return false;
    }

    public boolean isMagnet(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.startsWith("magnet:")
                || lower.startsWith("urn:btih:")
                || lower.contains("xt=urn:btih:");
    }

    public Map<String, Object> parseMagnetInfo(String url) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (url == null || url.isEmpty()) return map;
        String raw = url.trim();
        map.put("raw", raw);
        
        Matcher xm = Pattern.compile("xt=urn:btih:([0-9a-fA-F]{32,})", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (xm.find()) {
            map.put("info_hash", xm.group(1).toLowerCase());
        }
        
        Matcher dm = Pattern.compile("[?&]dn=([^&]+)").matcher(raw);
        if (dm.find()) {
            try {
                map.put("displayName", java.net.URLDecoder.decode(dm.group(1), "UTF-8"));
            } catch (Exception e) {
                map.put("displayName", dm.group(1));
            }
        }
        
        List<String> trackers = new ArrayList<>();
        Matcher tm = Pattern.compile("[?&]tr=([^&]+)").matcher(raw);
        while (tm.find()) {
            try {
                trackers.add(java.net.URLDecoder.decode(tm.group(1), "UTF-8"));
            } catch (Exception e) {
                trackers.add(tm.group(1));
            }
        }
        if (!trackers.isEmpty()) map.put("trackers", trackers);
        return map;
    }

    public String normalizeMagnet(String url) {
        if (url == null) return "";
        String s = url.trim();
        if (s.isEmpty()) return "";
        
        String lower = s.toLowerCase();
        if (!lower.startsWith("magnet:")) {
            if (lower.startsWith("urn:btih:")) {
                s = "magnet:?xt=" + s;
            } else if (lower.contains("xt=urn:btih:")) {
                s = "magnet:?" + s.replaceAll("^[?&]+", "");
            } else {
                return s;
            }
        }
        
        String[] segments = s.split("[?&]");
        StringBuilder sb = new StringBuilder();
        sb.append("magnet:?");
        Set<String> seen = new HashSet<>();
        boolean hasXt = false;
        for (String seg : segments) {
            if (seg.isEmpty() || seg.equals("magnet:")) continue;
            String key = seg.contains("=") ? seg.substring(0, seg.indexOf('=')).toLowerCase() : seg.toLowerCase();
            if (key.equals("xt") || key.startsWith("xt=")) {
                if (hasXt) continue;
                hasXt = true;
            }
            if (key.equals("tr")) {
                if (!seen.add("tr:" + seg)) continue;
            }
            
            String value = seg;
            if (key.equals("dn")) {
                String v = seg.substring(seg.indexOf('=') + 1);
                try {
                    value = "dn=" + java.net.URLEncoder.encode(
                            java.net.URLDecoder.decode(v, "UTF-8"), "UTF-8");
                } catch (Exception e) {
                    value = seg;
                }
            }
            if (sb.length() > "magnet:?".length()) sb.append("&");
            sb.append(value);
        }
        return sb.toString();
    }

    public String extractMagnet(String text) {
        if (text == null || text.isEmpty()) return text;
        Matcher m = Pattern.compile("magnet:\\?[^\\s\"'<>\\\\]+", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return normalizeMagnet(m.group(0));
        }
        Matcher u = Pattern.compile("urn:btih:[0-9a-fA-F]{32,}").matcher(text);
        if (u.find()) {
            return normalizeMagnet(u.group(0));
        }
        return text;
    }

}
