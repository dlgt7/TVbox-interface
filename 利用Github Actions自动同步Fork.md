Fork其他大神的github项目
fork别人的github项目，然后更改默认branch为主要活动branch

源项目默认的主branch基本都是空，为了伪装屏蔽

添加Github Actions
点击Actions

新建自己的actions

添加如下代码[1]

![微信图片_20230620055616](https://github.com/dlgt7/TVbox-interface/assets/102397160/cb514d9b-2cf1-427b-b7e1-46d5eb27faa3)


# #.github/workflows/sync.yml
name: Sync Fork

on:
  push: # push 时触发, 主要是为了测试配置有没有问题
  schedule:
    - cron: '* */3 * * *' # 每3小时触发, 对于一些更新不那么频繁的项目可以设置为每天一次, 低碳一点
jobs:
  repo-sync:
    runs-on: ubuntu-latest
    steps:
      - uses: TG908/fork-sync@v1.6.3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          owner: ZhaoUncle # fork 的上游仓库 user
          head: main # fork 的上游仓库 branch
          base: main # 本地仓库 branch
          
owner: 填写上游的项目作者名

head: 上游仓库 branch

base: 本地仓库 branch

提交action commit

坐等Github Actions自动更新
