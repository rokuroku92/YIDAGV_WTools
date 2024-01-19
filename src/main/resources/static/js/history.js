
document.addEventListener("DOMContentLoaded", async function() {
    try {
        await setTasks();
        await setNotifications();
    } catch (error) {
        console.error('發生錯誤：', error);
    }
});

function setTasks() {
    return new Promise((resolve, reject) => {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', baseUrl + "/api/task/allTaskDetails", true);
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
        var status;

        if(tasks[i].status == -1)
            status = "canceled";
        else if(tasks[i].status == 100)
            status= 'completed';
        else if(tasks[i].status == 0)
            status= 'waiting';
        else
            status= 'executing';

        if(tasks[i].title == "Elevator"){
            tasksHTML += `<div class="row task">
                <div class="col agvTask">
                    <div class="row py-1">
                        <div class="col-6 p-0">
                            <div class="row taskTitle">
                                <div class="col">
                                    <p>${tasks[i].taskNumber}:${tasks[i].sequence}</p>
                                </div>
                            </div>
                            <div class="row taskContent">
                                <div class="col">
                                    <p>${tasks[i].title} | Mode: ${tasks[i].modeMemo} </p>
                                </div>
                            </div>
                        </div>
                        <div class="col-6 p-0">
                            <div class="row taskTB">
                                <div class="col-12 p-0">
                                    <div class="row">
                                        <div class="col-5 taskstatus ${status}">${status}</div>
                                        <div class="col-7 tasktime">${year}/${month}/${day}&nbsp;${hour}:${minute}:${second}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
        } else {
            tasksHTML += `<div class="row task">
                <div class="col agvTask">
                    <div class="row py-1">
                        <div class="col-6 p-0">
                            <div class="row taskTitle">
                                <div class="col">
                                    <p>${tasks[i].taskNumber}:${tasks[i].sequence}</p>
                                </div>
                            </div>
                            <div class="row taskContent">
                                <div class="col">
                                    <p>AGV: ${tasks[i].title} | Start: ${tasks[i].start} | End: ${tasks[i].terminal}</p>
                                </div>
                            </div>
                        </div>
                        <div class="col-6 p-0">
                            <div class="row taskTB">
                                <div class="col-12 p-0">
                                    <div class="row">
                                        <div class="col-5 taskstatus ${status}">${status}</div>
                                        <div class="col-7 tasktime">${year}/${month}/${day}&nbsp;${hour}:${minute}:${second}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
        }
        
    }
    document.getElementById("taskQueue").innerHTML = tasksHTML;
}

function setNotifications() {
    return new Promise((resolve, reject) => {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', baseUrl + "/api/homepage/notifications", true);
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
                    '<label class="messageTitle">'+notifications[i].name+'</label><label class="messageContent">'+notifications[i].message+'</label><label class="messageTime">'+year+"/"+month+"/"+day+'&nbsp;'+hour+":"+minute+":"+second+'</label></div></div></div>';
    }
    document.getElementById("notification").innerHTML = notificationsHTML;
}
