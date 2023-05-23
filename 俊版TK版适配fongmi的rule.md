ApiConfig.java    TK版505  俊版446

  //video parse rule for host  修改获取接口里的rules配置字段名，适配fongmi配置。20230523

                JsonObject obj = (JsonObject) oneHostRule;    
         //start
         String host = "";
                if (obj.has("hosts")) {
                    JsonArray hostsArray = obj.getAsJsonArray("hosts");
                    if (hostsArray.size() > 1) {
                        host = hostsArray.get(0).getAsString().trim(); // use first value of hosts array
                    }else{
                        host = obj.get("hosts").getAsString();
                       }                      
                    }else{
                        host = obj.get("host").getAsString();                    
                  }

                if (obj.has("regex")) {
                    JsonArray ruleJsonArr = obj.getAsJsonArray("regex");
                    ArrayList<String> regex = new ArrayList<>();
                    for(JsonElement one : ruleJsonArr) {
                        String oneRule = one.getAsString();
                        regex.add(oneRule);
                    }
                    if (regex.size() > 0) {
                        VideoParseRuler.addHostRule(host, regex);
                    }
                }
            //end


    if (obj.has("rule")) {
                    JsonArray ruleJsonArr = obj.getAsJsonArray("rule");
