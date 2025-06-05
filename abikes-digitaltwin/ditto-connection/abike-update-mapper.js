function mapToDittoProtocolMsg(headers, textPayload, bytePayload, contentType) {
    var jsonData;
    try {
        jsonData = JSON.parse(textPayload);
    } catch (e) {
        return null;
    }
    var thingId = jsonData.id;
    if (!thingId) return null;

    var features = {};
    for (var key in jsonData)
        if (jsonData.hasOwnProperty(key)) features[key] = {
            properties: {
                value: jsonData[key]
            }
        };
    var topic = headers['kafka.topic'] || '';
    return Ditto.buildDittoProtocolMsg('default', thingId, 'things', 'twin', 'commands', 'modify', '/features', headers, features);
}