/* This specific file is completely open-source,
 * comes without copyright and you can freely
 * copy-paste it wherever you want or do whatever
 * you want with it.
 */

function wdExecRequest(name, func) {
	window.mcefQuery({	request: "WebDisplays_" + name,
						persistent: true,
						onSuccess: function(response) {
							try {
								func(JSON.parse(response));
							} catch(e) {
								document.write(response + "<br/>" + e);
							}
						},
						onFailure: function(errCode, errMsg) {
							document.write(errMsg);
						}});
}

function wdGetSize(callback) {
	wdExecRequest("GetSize", function(size) {
		callback(size.x, size.y);
	});
}

function wdGetRedstoneAt(x, y, callback) {
	wdExecRequest("GetRedstoneAt(" + x + "," + y + ")", function(resp) {
		callback(resp.level);
	});
}

function wdGetRedstoneArray(callback) {
	wdExecRequest("GetRedstoneArray", function(resp) {
		callback(resp.levels);
	});
}

function wdClearRedstone() {
	wdExecRequest("ClearRedstone", function(resp) { /* console.log(resp.status); */ });
}

function wdSetRedstoneAt(x, y, state) {
	var istate = state ? 1 : 0;
	wdExecRequest("SetRedstoneAt(" + x + "," + y + "," + istate + ")", function(resp) { /* console.log(resp.status); */ });
}
