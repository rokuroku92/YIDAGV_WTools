var xhr = new XMLHttpRequest();
var baseUrl = window.location.origin + "/YIDDemoRoom";

window.onload = async function(){
    try {
        await setAGVList();
        await setStationList();
    } catch (error) {
        console.error('發生錯誤：', error);
    }
}

function setAGVList() {
    return new Promise((resolve, reject) => {
        xhr.open('GET', baseUrl + "/api/homepage/agvlist", true);
        xhr.send();
        xhr.onload = function(){
            if(xhr.status == 200){
                addAGVList(JSON.parse(this.responseText));
                resolve(); // 解析成功时，将 Promise 设置为已完成状态
            }else {
                reject(new Error('AGV列表獲取失敗')); // 解析失败时，将 Promise 设置为拒绝状态
            }
        };
    });
}

function addAGVList(agvList){
    var agvListHTML="";
    for(let i=0;i<agvList.length;i++){
        agvListHTML += '<div data-id="'+(i+1)+'" class="col-auto sendCard agv" onclick="select(this)">' +
                        '<div class="row"><div class="col cardInfo"><div class="row"><div class="col">' +
                        '<p class="title AGVTitle" id="AGV'+(i+1)+'Title">'+agvList[i].name+'</p></div></div><div class="row"><div class="col">' +
                        '<p class="AGVName" id="AGV'+(i+1)+'Name">'+agvList[i].memo+'</p></div></div></div><div class="col cardImg">' +
                        '<img class="img-fluid ratio ratio-1x1" src="'+baseUrl+'/image/agv_250.png" alt="image error"></div></div></div>';
    }
    document.getElementById('agvList').innerHTML = agvListHTML;
}

function setStationList() {
    return new Promise((resolve, reject) => {
        xhr.open('GET', baseUrl + "/api/homepage/stations", true);
        xhr.send();
        xhr.onload = function(){
            if(xhr.status == 200){
                addStationList(JSON.parse(this.responseText));
                resolve(); // 解析成功时，将 Promise 设置为已完成状态
            }else {
                reject(new Error('start列表獲取失敗')); // 解析失败时，将 Promise 设置为拒绝状态
            }
        };
    });
}

function addStationList(stationList){
    var startListHTML="";
    var terminalListHTML="";
    for(let i=0;i<stationList.length;i++){
        startListHTML += '<div data-id="'+stationList[i].id+'" class="col-auto sendCard station start" onclick="select(this)">' +
                         '<div class="row"><div class="col cardInfo"><div class="row"><div class="col"><p class="title stationTitle">' +
                         stationList[i].name + '</p></div></div><div class="row"><div class="col"><p class="stationContent">' +
                         stationList[i].memo + '</p></div></div></div></div></div>';

        terminalListHTML += '<div data-id="'+stationList[i].id+'" class="col-auto sendCard station terminal" onclick="select(this)">' +
                            '<div class="row"><div class="col cardInfo"><div class="row"><div class="col"><p class="title stationTitle">' +
                            stationList[i].name + '</p></div></div><div class="row"><div class="col"><p class="stationContent">' +
                            stationList[i].memo + '</p></div></div></div></div></div>';
    }
    document.getElementById('startST').innerHTML = startListHTML;
    document.getElementById('terminalST').innerHTML = terminalListHTML;
}


var sendContent = {agv: "", start: "", terminal: "", mode: ""}
function select(obj){
    var objClass = "";
    var objColor = "#FFFFFF";
    if(obj.style.backgroundColor != "yellow")
        objColor = obj.style.backgroundColor;
    // console.log("objid: "+obj.getAttribute("id"));

    for(let i=0;i<obj.classList.length;i++){
        objClass += "." + obj.classList[i];
        switch (obj.classList[i]) {
            case "agv":
                sendContent.agv = obj.getAttribute("data-id");
                break;
            case "start":
                sendContent.start = obj.getAttribute("data-id");
                break;
            case "terminal":
                sendContent.terminal = obj.getAttribute("data-id");
                break;
            case "mode":
                sendContent.mode = obj.getAttribute("data-id");
                objColor = "#C9C9C9"
                break;
            default:
                break;
        }
    }
    var stations = document.querySelectorAll(objClass);
    // console.log("obj.classList: "+objClass);
    for(let i=0;i<stations.length;i++)
        stations[i].style.backgroundColor = objColor;

    obj.style.backgroundColor = "yellow";
    
}

async function sendTask(){
    var now = new Date();
    var nowTime = ""+now.getFullYear()+("0"+(now.getMonth()+1)).slice(-2)+("0"+now.getDate()).slice(-2)+
                    ("0"+now.getHours()).slice(-2)+("0"+now.getMinutes()).slice(-2)+("0"+now.getSeconds()).slice(-2);

    var cnt = ("Send Task contants:"+"\nAGV: "+sendContent.agv+"\nStart: "+sendContent.start+
                "\nTerminal: "+sendContent.terminal+"\nMode: "+sendContent.mode+"\nTime: "+nowTime);
    // alert(cnt);
    xhr.open('GET', baseUrl + "/api/sendTask?agv="+sendContent.agv+"&start="+sendContent.start+
                "&terminal="+sendContent.terminal+"&mode="+sendContent.mode+"&time="+nowTime, true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            if(this.responseText == 'OK')
                alert("成功發送任務\n"+cnt);
            else
                alert("發送任務失敗，可能是格式錯誤");
        }else
            alert("發送任務失敗");
    };
    window.location.reload();
    // console.log("Send Task contants:");
    // console.log("AGV: "+sendContent.agv);
    // console.log("Start: "+sendContent.start);
    // console.log("Terminal: "+sendContent.terminal);
    // console.log("Mode: "+sendContent.mode);
}

