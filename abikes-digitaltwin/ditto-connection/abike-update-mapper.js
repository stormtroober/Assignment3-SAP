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
    jsonData.id = thingId;
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
        thingId: "abike:" + thingId,
        policyId: "org.eclipse.ditto:simple-policy",
        features: features
    };

    return Ditto.buildDittoProtocolMsg(
        "org.eclipse.ditto",
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
