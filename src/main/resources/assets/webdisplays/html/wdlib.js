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

function wdGetUpgrades(callback) {
	wdExecRequest("GetUpgrades", function(resp) {
		callback(resp.upgrades);
	});
}

function wdIsOwner(callback) {
	wdExecRequest("IsOwner", function(resp) {
		callback(resp.isOwner);
	});
}

//Requires upgrade: webdisplays:redinput
function wdGetRedstoneAt(x, y, callback) {
	wdExecRequest("GetRedstoneAt(" + x + "," + y + ")", function(resp) {
		callback(resp.level);
	});
}

//Requires upgrade: webdisplays:redinput
function wdGetRedstoneArray(callback) {
	wdExecRequest("GetRedstoneArray", function(resp) {
		callback(resp.levels);
	});
}

//Requires upgrade: webdisplays:redoutput
//If the client is not the owner, does nothing (resp.status = "notOwner")
function wdClearRedstone() {
	wdExecRequest("ClearRedstone", function(resp) { /* console.log(resp.status); */ });
}

//Requires upgrade: webdisplays:redoutput
//If the client is not the owner, does nothing (resp.status = "notOwner")
function wdSetRedstoneAt(x, y, state) {
	var istate = state ? 1 : 0;
	wdExecRequest("SetRedstoneAt(" + x + "," + y + "," + istate + ")", function(resp) { /* console.log(resp.status); */ });
}

//Requires upgrade: webdisplays:gps
function wdGetLocation(callback) {
	wdExecRequest("GetLocation", function(resp) {
		callback(resp.x, resp.y, resp.z, resp.side);
	});
}
