/*jslint white: false */
/*global window, $, Util, RFB, */

function updateState(rfb, state, oldstate, msg) {
  var s, sb, cad, level;
  s = $D('noVNC_status');
  sb = $D('noVNC_status_bar');
  cad = $D('sendCtrlAltDelButton');
  switch (state) {
    case 'failed':       level = "error";  break;
    case 'fatal':        level = "error";  break;
    case 'normal':       level = "info"; break;
    case 'disconnected': level = "info"; break;
    case 'loaded':       level = "info"; break;
    default:             level = "warn";   break;
  }

  if (state === "normal" && cad) { cad.disabled = false; }
  else                    { if (cad) cad.disabled = true; }

  if (typeof(msg) !== 'undefined' && sb && s) {
    s.setAttribute("class", "alert alert-" + level);
    s.innerHTML = msg;
  }
}

function sendCtrlAltDel() {
  rfb.sendCtrlAltDel();
  return false;
}

$(function () {
  var fifoApiEndpoint = 'api/' + (Config.apiVersion || '1');
  var path;

  $D('sendCtrlAltDelButton').style.display = "inline";
  $D('sendCtrlAltDelButton').onclick = sendCtrlAltDel;

  // By default, use the host and port of server that served this file

  var uuid = $(document).getUrlParam("uuid");
  var ott = $(document).getUrlParam("ott");
  var host = Config["ws"].replace(/wss?:\/\//, "") || window.location.host;
  var port = "";//$(document).getUrlParam("port") || WebUtil.getQueryVar('port', window.location.port);
  if (port == "") {
    if (window.location.protocol === "https:") {
      var port = "443";
    } else {
      var port = "80";
    }
  }

  document.title = "VNC - " + uuid;

  path = fifoApiEndpoint + "/vms/" + uuid + "/vnc?fifo_ott=" + ott;
  rfb = new RFB({'target':       $D('noVNC_canvas'),
                 'encrypt':      (window.location.protocol === "https:"),
                 'true_color':   true,
                 'local_cursor': true,
                 'shared':       true,
                 'view_only':    false,
                 'updateState':  updateState});
  rfb.connect(host, port, "", path);
});
