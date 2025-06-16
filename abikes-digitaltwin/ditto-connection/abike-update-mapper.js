function mapToDittoProtocolMsg(headers, textPayload, bytePayload, contentType) {
    var jsonData;
    try {
        jsonData = JSON.parse(textPayload);
    } catch (e) {
        return null;
    }

    var topic = headers["kafka.topic"] || "";
    let thingId;
    if (topic === "abike-update") {
        thingId = "abike:" + jsonData.id;
    } else if( topic === "station-update") {
        thingId = "station:" + jsonData.id;
    }
    
    // Transform properties into Ditto features
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
