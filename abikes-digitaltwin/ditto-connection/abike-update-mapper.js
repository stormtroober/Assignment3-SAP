function mapToDittoProtocolMsg(headers, textPayload, bytePayload, contentType) {
    var jsonData;
    try {
        jsonData = JSON.parse(textPayload);
    } catch (e) {
        return null;
    }
    var thingId = "abike:" + jsonData.id;
    if (!thingId) return null;

    var features = {};
    for (var key in jsonData) {
        if (jsonData.hasOwnProperty(key) && key !== 'id') {
            features[key] = {
                properties: {
                    value: jsonData[key]
                }
            };
        }
    }

    var value = {
        policyId: "org.eclipse.ditto:simple-policy",
        features: features
    };

    return Ditto.buildDittoProtocolMsg(
        "",
        thingId,
        "things",
        "twin",
        "commands",
        "modify",
        "/",
        headers,
        value
    );
}
