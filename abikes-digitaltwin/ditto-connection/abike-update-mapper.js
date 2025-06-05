function mapToDittoProtocolMsg(headers, textPayload, bytePayload, contentType) {
    if (contentType !== 'application/json') return null;
    var jsonData;
    try {
        jsonData = JSON.parse(textPayload);
    } catch (e) {
        return null;
    }
    var thingId = jsonData.thingId || jsonData.id || headers['device_id'] || headers['ce-id'];
    if (!thingId) return null;
    delete jsonData.thingId;
    delete jsonData.id;
    var features = {};
    for (var key in jsonData)
        if (jsonData.hasOwnProperty(key)) features[key] = {
            properties: {
                value: jsonData[key]
            }
        };
    var topic = headers['kafka.topic'] || '';
    return Ditto.buildDittoProtocolMsg('default', thingId, 'things', 'twin', 'commands', 'modify', '/features', headers, features, {
        topic: topic
    });
}