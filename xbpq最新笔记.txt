1. XBPQ 是什么
①、属于 TVBox 家族里的「csp_XBPQ」引擎，纯字符串规则写法，无需 Python/JS 基础。
②、同一条规则既能让点播 App（TVBox、影视仓、Pluto）直接刮削网页，也能让直播源自动分组、嗅探。
③、与 drpy、py 相比：学习成本最低，调试最快；与 JSON 直连相比：可逆向无接口站点。
2. 核心语法一张图
① 截取        ：  起点&&终点  
② 二次截取    ：  起点&&终点||新起点&&新终点  
③ 拼接        ：  '+'  前后可混写字符串 / 变量  
④ 分类指定    ：  默认–a&&b||电影–c&&d||电视剧–e&&f  
⑤ JSON        ：  data.list[1].name   下标从 1 开始  
⑥ 转义        ：  保留字 $ # & * [ ] 前加 \\
⑦ 变量        ：  {cateId} {catePg} {area} {year} {class} {by} {act} {wd}  
⑧ 筛选        ：  类型$类型值#剧情$剧情值 …  
⑨ 嗅探词      ：  多个用 # 分隔，如 m3u8#mp4#flv  
⑩ 链接前缀/后缀： 自动补全相对路径
3. 字段清单（ext 内全部）
   | 字段            | 说明       | 示例                                                                         |   |                   |
| ------------- | -------- | -------------------------------------------------------------------------- | - | ----------------- |
| 主页url         | 入口       | `https://www.xxx.com`                                                      |   |                   |
| 分类url         | 列表页      | `https://www.xxx.com/vodshow/{cateId}--{area}------{catePg}---{year}.html` |   |                   |
| 分类            | 自建频道     | `电影$1#电视剧$2#综艺$3#动漫$4`                                                     |   |                   |
| 筛选            | 可留空，自动识别 | `外部json 或 直接写{}`                                                           |   |                   |
| 数组            | 列表循环体    | `<div class="stui-vodlist__box">&&</div>`                                  |   |                   |
| 标题            | 片名       | `title="&&"`                                                               |   |                   |
| 图片            | 封面       | `data-original="&&"`                                                       |   |                   |
| 链接            | 详情页      | `href="&&"`                                                                |   |                   |
| 副标题           | 更新集数     | `<span class="pic-text text-right">&&</span>`                              |   |                   |
| 嗅探词           | 正片格式     | `m3u8#mp4#flv#akamaized.net`                                               |   |                   |
| 播放数组          | 播放线路块    | `class="stui-content__playlist clearfix">&&</ul>`                          |   |                   |
| 播放列表          | 单集       | `<a&&/a>`                                                                  |   |                   |
| 播放标题          | 线路名      | `h3&&/h3`                                                                  |   |                   |
| 播放链接          | 真实地址     | `href="&&"`                                                                |   |                   |
| 直接播放          | 直播开关     | `1` 开启，`0` 关闭                                                              |   |                   |
| 链接前缀/后缀       | 补全       | `http://cdn.xxx.com` / `.m3u8`                                             |   |                   |
| 搜索url         | 搜索入口     | `https://www.xxx.com/search.php?wd={wd}`                                   |   |                   |
| 搜索数组/标题/图片/链接 | 同上逻辑     | …                                                                          |   |                   |
| 二次截取          | 不同页面不同规则 | \`电影–<ul>&&</ul>                                                           |   | 电视剧–<ol>&&</ol>\` |
4. 极速模板（直接改 3 处即可上线）
{
  "key": "极速演示",
  "name": "极速┃BPQ",
  "type": 3,
  "api": "csp_XBPQ",
  "searchable": 1,
  "quickSearch": 1,
  "filterable": 1,
  "ext": {
    "主页url": "https://www.你的目标站.com",
    "分类url": "https://www.你的目标站.com/vodshow/{cateId}--------{catePg}---.html",
    "分类": "电影$1#电视剧$2#综艺$3#动漫$4",
    "数组": "<div class=\"stui-vodlist__box\">&&</div>",
    "标题": "title=\"&&\"",
    "图片": "data-original=\"&&\"",
    "链接": "href=\"&&\"",
    "副标题": "<span class=\"pic-text text-right\">&&</span>",
    "播放数组": "class=\"stui-content__playlist clearfix\">&&</ul>",
    "播放列表": "<a&&/a>",
    "播放链接": "href=\"&&\"",
    "嗅探词": "m3u8#mp4#flv"
  }
}
