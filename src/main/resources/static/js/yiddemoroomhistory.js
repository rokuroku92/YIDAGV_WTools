var xhr = new XMLHttpRequest();
var baseUrl = window.location.origin + "/YIDDemoRoom";
var agvList;
var station;

window.onload = async function(){
    try {
        await getAGVList();
        await getStations();
        await setTasks();
        await setNotifications();
    } catch (error) {
        console.error('發生錯誤：', error);
    }
}

function getAGVList() {
    return new Promise((resolve, reject) => {
        xhr.open('GET', baseUrl + "/api/homepage/agvlist", true);
        xhr.send();
        xhr.onload = function(){
            if(xhr.status == 200){
                agvList = JSON.parse(this.responseText);
                resolve(); // 解析成功时，将 Promise 设置为已完成状态
            }else {
                reject(new Error('AGV列表獲取失敗')); // 解析失败时，将 Promise 设置为拒绝状态
            }
        };
    });
}

function getStations() {
    return new Promise((resolve, reject) => {
        xhr.open('GET', baseUrl + "/api/homepage/stations", true);
        xhr.send();
        xhr.onload = function(){
            if(xhr.status == 200){
                stations = JSON.parse(this.responseText);
                // console.log(stations);
                resolve(); // 解析成功时，将 Promise 设置为已完成状态
            }else {
                reject(new Error('起始站點資訊獲取失敗')); // 解析失败时，将 Promise 设置为拒绝状态
            }
        };
    });
}

function setTasks() {
    return new Promise((resolve, reject) => {
        xhr.open('GET', baseUrl + "/api/homepage/tasks", true);
        xhr.send();
        xhr.onload = function(){
            if(xhr.status == 200){
                var tasks = JSON.parse(this.responseText);
                addTasks(tasks);
                resolve(); // 解析成功時，將 Promise 設置為已完成狀態
            }else {
                reject(new Error('任務列表獲取失敗')); // 解析失敗時，將 Promise 設置為拒絕狀態
            }
        };
    });
}

function addTasks(tasks){  // 更新資料
    var tasksHTML = "";
    for(let i=0;i<tasks.length;i++){
        var datetime = tasks[i].createTaskTime;
        var year = datetime.substring(0, 4);
        var month = datetime.substring(4, 6);
        var day = datetime.substring(6, 8);
        var hour = datetime.substring(8, 10);
        var minute = datetime.substring(10, 12);
        var second = datetime.substring(12, 14);
        var st;
        var status;
        if(tasks[i].startId)
            st = stations[tasks[i].startId-1].name;
        else 
            st= 'undefined';

        if(tasks[i].status == -1)
            status = "canceled";
        else if(tasks[i].status == 100)
            status= 'completed';
        else if(tasks[i].status == 0)
            status= 'waiting';
        else
            status= 'executing';
        tasksHTML += "<div class=\"row task\"><div class=\"col agvTask\"><div class=\"row\"><div class=\"col\"><div class=\"row taskTitle\"><div class=\"col\">" +
                    "<p>"+tasks[i].taskNumber+"</p></div></div><div class=\"row taskContent\"><div class=\"col\">" +
                    "<p>AGV: "+agvList[tasks[i].agvId-1].name+"</p><p>Start: "+st+"</p><p>End: "+stations[tasks[i].terminalId-1].name+"</p><p>Mode: "+ tasks[i].modeId +"</p></div></div></div><div class=\"col-6\"><div class=\"row taskTB\">" +
                    "<div class=\"col-10\"><div class=\"row\"><div class=\"col taskstatus "+ status +"\">"+ status +"</div><div class=\"col\"><div class=\"row\"><div class=\"col\"><labe class=\"right\">"+year+"/"+month+"/"+day+"</label></div></div><div class=\"row\">" +
                    "<div class=\"col\"><label class=\"right\">"+hour+":"+minute+":"+second+"</label></div></div></div></div></div><div class=\"col-2\"><button type=\"button\" onclick=\"cancelTask(\'"+tasks[i].taskNumber+"\')\" class=\"btn btn-danger right\">" +
                    "<svg width=\"16\" height=\"16\" viewBox=\"0 0 16 16\" style=\"fill: white;\"><use xlink:href=\"#trash\"/></svg></button></div></div></div></div></div></div>";
    }
    document.getElementById("taskQueue").innerHTML = tasksHTML;
}

function setNotifications() {
    return new Promise((resolve, reject) => {
        xhr.open('GET', baseUrl + "/api/homepage/notification/all", true);
        xhr.send();
        xhr.onload = function(){
            if(xhr.status == 200){
                var notifications = JSON.parse(this.responseText);
                addNotifications(notifications);
                resolve(); // 解析成功時，將 Promise 設置為已完成狀態
            }else {
                reject(new Error('通知列表獲取失敗')); // 解析失敗時，將 Promise 設置為拒絕狀態
            }
        };
    });
}

function addNotifications(notifications){  // 更新資料
    var notificationsHTML = "";
    for(let i=0;i<notifications.length;i++){
        var datetime = notifications[i].createTime;
        var year = datetime.substring(0, 4);
        var month = datetime.substring(4, 6);
        var day = datetime.substring(6, 8);
        var hour = datetime.substring(8, 10);
        var minute = datetime.substring(10, 12);
        var second = datetime.substring(12, 14);
        var level;
        switch (notifications[i].level) {
            case 0:
                level = 'normal';
                break;
            case 1:
                level = 'info';
                break;
            case 2:
                level = 'warning';
                break;
            case 3:
                level = 'danger';
                break;
            default:
                level = 'normal';
                break;
        }
        notificationsHTML += '<div class="row"><div class="col message"><div class="nfStatus '+level+'"></div><div class="messageContentDiv">' +
                    '<label class="messageTitle">'+notifications[i].title+'</label><label class="messageContent">'+notifications[i].content+'</label><label class="messageTime">'+year+"/"+month+"/"+day+'&nbsp;'+hour+":"+minute+":"+second+'</label></div></div></div>';
    }
    document.getElementById("notification").innerHTML = notificationsHTML;
}

function cancelTask(taskNumber){
    xhr.open('GET', baseUrl + "/api/cancelTask?taskNumber=%23" + taskNumber.slice(1), true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            if(this.responseText == 'OK')
                alert("成功取消任務: ", taskNumber);
            else
                alert("取消任務失敗，可能是格式錯誤");
        }else
            alert("取消任務失敗");
        window.location.reload();
    };

}