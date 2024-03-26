var lastTaskListHTML;
var lastNotificationHTML = "";

document.addEventListener("DOMContentLoaded",async function() {
    try {
        await setAGVList();
        // await setAGVListBatteryChart();
        // await getStations();
        // await getModes();
        // await setTasks();
        // await setNotifications();
    } catch (error) {
        console.error('發生錯誤：', error);
    }
    updateAGVStatus();  //  取得狀態資料
    updateTaskLists();
    updateNotifications();
    setInterval(updateAGVStatus, 1000);  //  每秒更新
    setInterval(updateTaskLists, 1000);
    setInterval(updateNotifications, 1000);
});

function setAGVList() {
    return new Promise((resolve, reject) => {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', baseUrl + "/api/homepage/agvlist", true);
        xhr.send();
        xhr.onload = function(){
            if(xhr.status == 200){
                agvList = JSON.parse(this.responseText);
                addAGVList(agvList);
                resolve(); // 解析成功时，将 Promise 设置为已完成状态
            }else {
                reject(new Error('AGV列表獲取失敗')); // 解析失败时，将 Promise 设置为拒绝状态
            }
        };
    });
}

function addAGVList(agvList){  // 更新資料
    var agvListHTML = "";
// "<div class="dstatus obstacle"></div>
//                     <p class="agvStatus obstacle">obstacle</p>
    for(let i=0;i<agvList.length;i++){
        const memo = agvList[i].memo.split("-");
        agvListHTML += `<div class="col mx-2 agvCard">
        <div class="row center" style="width:100%; height: 100%;">
          <!-- <div class="dstatus normal" id="AGVStatusDiv${i+1}"></div> -->
          <div class="dstatus error" id="AGVStatusDiv${i+1}"></div>
          <div class="col">
            <div class="row cardTop">
              <div class="col-6 cardInfo">
                  <div class="row" style="padding-top: 1.5%;">
                      <div class="col" style=" display: inline-flex;align-items: center;">
                          <p class="title AGVTitle" id="AGVName${i+1}">${agvList[i].name}</p>
                      </div>
                  </div>
                  <div class="row">
                      <div class="col">
                          <label class="AGVMemo" id="AGVMemo${i+1}">${memo[0]}</label>
                          <label class="AGVMemo" style="margin-bottom: 0.5em;">${memo[1]}</label>
                      </div>
                  </div>
                  <div class="row">
                    <div class="col AGVstatus">
                      <label class="agvStatus error" id="AGVStatus${i+1}">OFFLINE</label>
                    </div>
                  </div>
              </div>
              <div class="col-3 center AGVImg">
                <img id="AGVImg${i+1}" class="img-fluid" style="width: 100%;" src="image/${agvList[i].img}" alt="image error">
              </div>
              <div class="col-3">
                <div class="row">
                  <div class="col center" style="padding-top: 1rem;padding-bottom: 1rem;">
                    <svg style="fill: #FFFFFF;" width="40" height="40">
                      <use id="AGVSignalSvg${i+1}" xlink:href="#wifi-0"/>
                    </svg>
                  </div>
                </div>
                <div class="row">
                  <div class="col center">
                    <div class="row cardBattery">
                      <div class="col-12 center">
                        <svg fill="white" width="50" height="30">
                          <use id="AGVBatterySvg${i+1}" xlink:href="#battery-0"/>
                        </svg>
                      </div>
                      <div class="col center">
                        <label id="AGVBattery${i+1}">0%</label>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
            </div>
            <div class="row cardBottom">
              <div class="col-12">
                <div class="agvTask nowTask">
                  <div class="row agvTask-row">
                    <div class="col-12">
                      <div class="row taskTitle">
                        <div class="col">
                          <p id="AGVTaskNumber${i+1}"></p>
                        </div>
                      </div>
                      <div class="row taskContent">
                        <div class="col">
                          <nobr><p id="AGVTaskST${i+1}"></p></nobr>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
        
      </div>`;
    }
    document.getElementById("agvList").innerHTML = agvListHTML;
}

function updateAGVStatus() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/homepage/agv", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var agv = JSON.parse(this.responseText);
            agvUpdate(agv);
        }
    };
}

function agvUpdate(agv){  // 更新資料
    for(let i=0;i<agv.length;i++){ // agv遍歷
        // 更新電量百分比
        var batteryString = "AGVBattery"+(i+1);
        document.getElementById(batteryString).innerHTML = agv[i].battery+"%";  
        // 更新電量
        batteryString = "AGVBatterySvg"+(i+1);
        var agvBattery = document.getElementById(batteryString);
        // agv[i].charging == 0 ? agvBattery.setAttribute("xlink:href", "#battery-charging") :
        agv[i].battery > 90 ? agvBattery.setAttribute("xlink:href", "#battery-6") :
            agv[i].battery > 80 ? agvBattery.setAttribute("xlink:href", "#battery-5") : 
            agv[i].battery > 60 ? agvBattery.setAttribute("xlink:href", "#battery-4") :
            agv[i].battery > 40 ? agvBattery.setAttribute("xlink:href", "#battery-3") :
            agv[i].battery > 20 ? agvBattery.setAttribute("xlink:href", "#battery-2") :
            agv[i].battery > 0 ? agvBattery.setAttribute("xlink:href", "#battery-1") :
            agvBattery.setAttribute("xlink:href", "#battery-0");
        // 更新電量圓餅圖
        /* 20230703已改為使用電量svg
        var updateBattery = [agv[i].battery, 100-agv[i].battery]; 
        var agvBatteryChart = Chart.instances[i];
        if(agv[i].battery < 50)
            agvBatteryChart.data.datasets[0].backgroundColor = ['rgb(255, 0, 0)','rgb(255, 255, 255)'];
        else
            agvBatteryChart.data.datasets[0].backgroundColor = ['rgb(118, 212, 114)','rgb(255, 255, 255)'];
        agvBatteryChart.data.datasets[0].data = updateBattery;
        agvBatteryChart.update();
        */


        // 更新信號強度
        var signalString = "AGVSignalSvg"+(i+1);
        var agvSignal = document.getElementById(signalString);
        agv[i].signal > 90 ? agvSignal.setAttribute("xlink:href", "#wifi-4") : 
            agv[i].signal > 75 ? agvSignal.setAttribute("xlink:href", "#wifi-3") :
            agv[i].signal > 50 ? agvSignal.setAttribute("xlink:href", "#wifi-2") :
            agv[i].signal > 25 ? agvSignal.setAttribute("xlink:href", "#wifi-1") :
            agvSignal.setAttribute("xlink:href", "#wifi-0");
        
        // 更新AGV狀態
        // agv[i].task(任務號碼)未使用
        document.getElementById('AGVStatusDiv'+(i+1)).classList.remove('error', 'warning', 'normal');
        document.getElementById('AGVStatus'+(i+1)).classList.remove('error', 'warning', 'normal');
        /*
        OFFLINE(1), ONLINE(2), MANUAL(3), REBOOT(4),
        STOP(5), DERAIL(6), COLLIDE(7), OBSTACLE(8),
        EXCESSIVE_TURN_ANGLE(9), WRONG_TAG_NUMBER(10), UNKNOWN_TAG_NUMBER(11),
        EXCEPTION_EXCLUSION(12), SENSOR_ERROR(13), CHARGE_ERROR(14), ERROR_AGV_DATA(15);
        */
        var statusHTMLClass = "normal"; 
        var statusText = agv[i].status; 
        switch (agv[i].status) {
            case "OFFLINE":
                statusHTMLClass = "error";
                break;
            case "ONLINE":
                if(agv[i].taskStatus == "NO_TASK"){
                    if(!agv[i].task){
                        statusHTMLClass = "warning";
                        statusText="IDLE";
                    } else {
                        // 回待命點的任務
                        statusText="GO_STANDBY";
                    }
                } else {
                    statusText="WORKING";
                }
                break;
            case "MANUAL":
                statusHTMLClass = "warning"; 
                break;
            case "REBOOT":
                statusHTMLClass = "error";
                break;
            case "STOP":
                statusHTMLClass = "error";
                break;
            case "DERAIL":
                statusHTMLClass = "error";
                break;
            case "COLLIDE":
                statusHTMLClass = "error"; 
                break;
            case "OBSTACLE":
                statusHTMLClass = "warning"; 
                break;
            case "EXCESSIVE_TURN_ANGLE":
                // statusText="WHEEL_ERROR";
                statusHTMLClass = "error";
                break;
            case "WRONG_TAG_NUMBER":
                statusHTMLClass = "error";
                break;
            case "UNKNOWN_TAG_NUMBER":
                statusHTMLClass = "error";
                break;
            case "EXCEPTION_EXCLUSION":
                statusHTMLClass = "error";
                break;
            case "SENSOR_ERROR":
                statusHTMLClass = "error";
                break;
            case "CHARGE_ERROR":
                statusHTMLClass = "error";
                break;
            case "NAVIGATION_LOST":
                statusHTMLClass = "error";
                break;
            case "ERROR_AGV_DATA":
                statusHTMLClass = "error";
                break;
            default:
                statusHTMLClass = "error";
                break;
        }
        document.getElementById('AGVStatusDiv'+(i+1)).classList.add(statusHTMLClass);
        document.getElementById('AGVStatus'+(i+1)).classList.add(statusHTMLClass);
        document.getElementById('AGVStatus'+(i+1)).innerHTML = statusText;

        if(agv[i].task){
            document.getElementById('AGVTaskNumber'+(i+1)).innerHTML = agv[i].task.taskNumber;
            let taskContent = "START: " + agv[i].task.startStation + " | TERMINAL: " + agv[i].task.terminalStation;
            document.getElementById('AGVTaskST'+(i+1)).innerHTML = taskContent;
        } else {
            document.getElementById('AGVTaskNumber'+(i+1)).innerHTML = "";
            document.getElementById('AGVTaskST'+(i+1)).innerHTML = "<h5>NO TASK</h5>";
        }

        // 更新AGV位置
        // updateAGVPositions(agv[i].place);
        
        // updateTN();
    }
    // 判斷是否警報
    agvIAlarm(agv);
}

function updateTaskLists() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/task/now", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var taskLists = JSON.parse(this.responseText);
            taskListsUpdate(taskLists);
        }
    };
}

function taskListsUpdate(taskLists) {
    var taskListsHTML = "";
    for(let i=0;i<taskLists.length;i++){
        taskListsHTML += `<div class="row task">
                            <div class="col agvTask">
                            <div class="row agvTask-row">
                                <div class="col-4">
                                <div class="row taskTitle">
                                    <div class="col">
                                    <label>${taskLists[i].taskNumber}</label>
                                    </div>
                                </div>
                                <div class="row taskContent">
                                    <div class="col">
                                    <label>任務階段: ${taskLists[i].phase}</label>
                                    </div>
                                </div>
                                </div>
                                <div class="col-4 center">
                                <div class="progress">
                                    <div class="progress-bar" role="progressbar" style="width: ${taskLists[i].progress}%;">${taskLists[i].progress}%</div>
                                </div>
                                </div>
                                <div class="col-2 steps">
                                <label>總任務數量: ${taskLists[i].steps}
                                </label>
                                </div>
                                <div class="col-2 center">
                                <button id="taskDetailBtn${taskLists[i].taskNumber.substring(1)}" type="button" class="btn btn-secondary task-btn" onclick="getDetails('${taskLists[i].taskNumber.substring(1)}')">Details</button>
                                <button id="taskDetailHideBtn${taskLists[i].taskNumber.substring(1)}" type="button" class="btn btn-secondary task-btn" style="display: none;" onclick="hideDetails('${taskLists[i].taskNumber.substring(1)}')">Hide</button>
                                ${taskLists[i].progress == 0 ? `<button type="button" class="btn btn-danger task-btn" onclick="cancelTask('${taskLists[i].taskNumber.substring(1)}')">Remove</button>` : ``}
                                </div>
                            </div>

                            <div class="row task-detail-row" style="display: none;" id="taskDetail${taskLists[i].taskNumber.substring(1)}">
                                <div class="col-12 center task-detail-content">
                                <table class="table" style="color: #FFFFFF;">
                                    <thead>
                                    <tr>
                                        <th>Sequence</th>
                                        <th>Title</th>
                                        <th>Start</th>
                                        <th>Terminal</th>
                                        <th>Mode</th>
                                        <th>Status</th>
                                    </tr>
                                    </thead>
                                    <tbody id="taskDetailTable${taskLists[i].taskNumber.substring(1)}"></tbody>
                                </table>
                                </div>
                            </div>

                            </div>
                        </div>`;
    }
    if(taskListsHTML !== lastTaskListHTML){
        document.getElementById("taskListQueue").innerHTML = taskListsHTML;
        lastTaskListHTML = taskListsHTML;
    }
}

function cancelTask(taskNumber){
    fetch(baseUrl+`/api/cancelTask?taskNumber=${taskNumber}`
    ).then(response => {
        return response.text();
    }).then(response => {
        alert(response);
    }).catch(error => {
        alert("取消任務失敗: ", error);
    });
}

function getDetails(taskNumber){
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/task/taskDetail?taskNumber=" + taskNumber, true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var taskDetails = JSON.parse(this.responseText);
            showDetails(taskNumber, taskDetails);
        }
    };
}

function showDetails(taskNumber, taskDetails){
    var taskDetailHTML = "";
    for(let i=0;i<taskDetails.length;i++){
        taskDetails[i].start = taskDetails[i].start == undefined ? "" : taskDetails[i].start;
        taskDetails[i].terminal = taskDetails[i].terminal == undefined ? "" : taskDetails[i].terminal;
        taskDetailHTML += `<tr>
                                    <th>` + taskDetails[i].sequence + `</td>
                                    <td>` + taskDetails[i].title + `</td>
                                    <td>` + taskDetails[i].start + `</td>
                                    <td>` + taskDetails[i].terminal + `</td>
                                    <td>` + taskDetails[i].modeMemo + `</td>
                                    <td>` + taskDetails[i].status + `</td>
                                </tr>`;
        // if(taskDetails[i].mode == 1){
        //     taskDetailHTML += `<tr>
        //                             <th>` + taskDetails[i].sequence + `</td>
        //                             <td>` + taskDetails[i].title + `</td>
        //                             <td>` + taskDetails[i].start + `</td>
        //                             <td>` + taskDetails[i].terminal + `</td>
        //                             <td>` + taskDetails[i].modeMemo + `</td>
        //                             <td>` + taskDetails[i].status + `</td>
        //                         </tr>`;
        // } else {
        //     taskDetailHTML += `<tr>
        //                             <th>` + taskDetails[i].sequence + `</td>
        //                             <td>` + taskDetails[i].title + `</td>
        //                             <td></td>
        //                             <td></td>
        //                             <td>` + taskDetails[i].modeMemo + `</td>
        //                             <td>` + taskDetails[i].status + `</td>
        //                         </tr>`;
        // }
    }
    var taskDetailBtnString = "taskDetailBtn" + taskNumber;
    var taskDetailHideBtnString = "taskDetailHideBtn" + taskNumber;
    var taskDetailString = "taskDetail" + taskNumber;
    var taskDetailTableString = "taskDetailTable" + taskNumber;
    document.getElementById(taskDetailBtnString).style.display = "none";
    document.getElementById(taskDetailHideBtnString).style.display = "block";
    document.getElementById(taskDetailString).style.display = "block";
    document.getElementById(taskDetailTableString).innerHTML = taskDetailHTML;
}

function hideDetails(taskNumber){
    var taskDetailBtnString = "taskDetailBtn" + taskNumber;
    var taskDetailHideBtnString = "taskDetailHideBtn" + taskNumber;
    var taskDetailString = "taskDetail" + taskNumber;
    document.getElementById(taskDetailBtnString).style.display = "block";
    document.getElementById(taskDetailHideBtnString).style.display = "none";
    document.getElementById(taskDetailString).style.display = "none";
}

function updateNotifications(){
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/homepage/notifications", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var notificationJson = JSON.parse(this.responseText);
            notificationUpdate(notificationJson);
        }
    };
}

function notificationUpdate(notificationJson){
    var notificationHTML = "";
    for(let i=0;i<notificationJson.length;i++){
        let datetime = notificationJson[i].createTime;
        let year = datetime.substring(0, 4);
        let month = datetime.substring(4, 6);
        let day = datetime.substring(6, 8);
        let hour = datetime.substring(8, 10);
        let minute = datetime.substring(10, 12);
        let second = datetime.substring(12, 14);

        const status = (() => {
            switch(notificationJson[i].level){
                case 0:
                    return "normal";
                case 1:
                    return "info";
                case 2:
                    return "warning";
                case 3:
                    return "danger";
                default:
                    return "normal";
            }
        })();
        notificationHTML += `<div class="row">
                                <div class="col message">
                                <div class="nfStatus ${status}"></div>
                                <div class="messageContent">
                                    <label>${notificationJson[i].name}</label>
                                    <p>${notificationJson[i].message}</p>
                                    <p style="float: right;margin: 0px;">${year}/${month}/${day}&nbsp;${hour}:${minute}:${second}</p>
                                </div>
                                </div>
                            </div>`;
    }
    if(lastNotificationHTML !== notificationHTML){
        document.getElementById("notification").innerHTML = notificationHTML;
        lastNotificationHTML = notificationHTML;
    }
}

var settingCloseAlarm = localStorage.getItem("closeAlarm") == 1 ? true : false;
var cancelAlarm = false;
var alarmToggle = true;
function agvIAlarm(agv) {
    if (settingCloseAlarm) {
        return;
    }
    // if (Notification.permission !== 'granted') {
    //     Notification.requestPermission();
    // }
    let cancelAlarmCnt = 0;
    for(let i=0;i<agv.length;i++){
        if (!agv[i].iAlarm) {
            document.getElementById(`alarmInfo${i+1}`).innerHTML = null;
            cancelAlarmCnt++;
            console.log(cancelAlarmCnt);
        } else {
            switch (agv[i].status) {
                case "STOP":
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} 觸發緊急停止，請至現場排除！`;
                    break;
                case "DERAIL":
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} 出軌，請至現場排除！`;
                    break;
                case "COLLIDE":
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} 發生碰撞，請至現場排除！`;
                    break;
                case "OBSTACLE":
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} 前有障礙超過30秒，請至現場排除！`;
                    break;
                case "EXCESSIVE_TURN_ANGLE":
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} 馬達驅動器異常，請至現場排除！`;
                    break;
                case "WRONG_TAG_NUMBER":
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} AMR系統找不到路徑(卡號錯誤)，請聯絡我們！`;
                    break;
                case "UNKNOWN_TAG_NUMBER":
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} AMR系統路徑未匹配(未知卡號)，請聯絡我們！`;
                    break;
                case "NAVIGATION_LOST":
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} AMR失去導航方向，請至現場協助將AMR推至啟動點！`;
                    break;
                default:
                    document.getElementById(`alarmInfo${i+1}`).innerHTML = `AMR#${agv[i].id} 後端資料錯誤： ${agv[i].status}`;
                    break;
            }
            if (!cancelAlarm) {
                if (document.getElementById('alarmModal').style.display != 'block') {
                    document.getElementById('alarmBTN').click();
                }
                if (alarmToggle) {
                    alarmToggle=false;
                    const audio = document.createElement("audio");
                    // audio.src = baseUrl+"/audio/laser.mp3";
                    audio.src = baseUrl+"/audio/alarm3.m4a";
                    audio.play();
                } else {
                    alarmToggle=true;
                }
            }
        }
    }
    if (cancelAlarmCnt == 3){
        if(document.getElementById('alarmModal').style.display == 'block'){
            document.getElementById('closeAlarmBTN').click();
        }
        cancelAlarm = false;
    }
    
}

function closeAlarm() {
    cancelAlarm = true;
}