 
app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java
       // Getting EPG Address
        epgStringAddress = Hawk.get(HawkConfig.EPG_URL, "");
        if (StringUtils.isBlank(epgStringAddress)) {
            epgStringAddress = "https://epg.112114.xyz/";
//            Hawk.put(HawkConfig.EPG_URL, epgStringAddress);

tk版178行 俊版204行
