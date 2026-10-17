package dev.kissoo.loader;

enum ClientProfile {
    FOREVER("Forever", "Forever Fabric", "1.21.1", "0.18.4", "0.116.7+1.21.1",
            "3.4.9", "https://cdn.modrinth.com/data/rIC2XJV4/versions/CW9eLvKd/ViaFabricPlus-3.4.9.jar",
            "baaee00a0b33a24b15b02d67f258df1f9a78398d", "ForeverClient-"),
    STRINOVA("Strinova", "Strinova Fabric", "1.21.4", "0.18.4", "0.119.4+1.21.4",
            "4.0.5-BACKPORT", "https://cdn.modrinth.com/data/rIC2XJV4/versions/8sPJsNcy/ViaFabricPlus-4.0.5-BACKPORT.jar",
            "33470639079be5bb0369beda8c5f31e37eb390f7", "StrinovaClient-");

    final String apiName;
    final String title;
    final String minecraft;
    final String loader;
    final String fabricApi;
    final String viaFabricPlus;
    final String viaFabricPlusUrl;
    final String viaFabricPlusSha1;
    final String jarPrefix;

    ClientProfile(String apiName, String title, String minecraft, String loader, String fabricApi,
                  String viaFabricPlus, String viaFabricPlusUrl, String viaFabricPlusSha1, String jarPrefix) {
        this.apiName = apiName;
        this.title = title;
        this.minecraft = minecraft;
        this.loader = loader;
        this.fabricApi = fabricApi;
        this.viaFabricPlus = viaFabricPlus;
        this.viaFabricPlusUrl = viaFabricPlusUrl;
        this.viaFabricPlusSha1 = viaFabricPlusSha1;
        this.jarPrefix = jarPrefix;
    }
}
