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

## 详解schedule cron 定时任务表达式

每天凌晨2点 0 0 2 * * ?和每天隔一小时 0 * */1 * * ?

例1：每隔5秒执行一次：*/5 * * * * ?

例2：每隔5分执行一次：0 */5 * * * ?

在26分、29分、33分执行一次：0 26,29,33 * * * ?

例3：每天半夜12点30分执行一次：0 30 0 * * ? （注意日期域为0不是24）

每天凌晨1点执行一次：0 0 1 * * ?

每天上午10：15执行一次： 0 15 10 ? * * 或 0 15 10 * * ? 或 0 15 10 * * ? *

每天中午十二点执行一次：0 0 12 * * ?

每天14点到14：59分，每1分钟执行一次：0 * 14 * * ?

每天14点到14：05分，每1分钟执行一次：0 0-5 14 * * ?

每天14点到14：55分，每5分钟执行一次：0 0/5 14 * * ?

每天14点到14：55分，和18点到18点55分，每5分钟执行一次：0 0/5 14,18 * * ?

每天18点执行一次：0 0 18 * * ?

每天18点、22点执行一次：0 0 18,22 * * ?

每天7点到23点，每整点执行一次：0 0 7-23 * * ?

每个整点执行一次：0 0 0/1 * * ?
