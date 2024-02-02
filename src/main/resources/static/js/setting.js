document.addEventListener("DOMContentLoaded", function() {
    getConfig();
    document.getElementById("gridManual").checked = localStorage.getItem("gridManual") == 1 ? true : false;
    document.getElementById("save").addEventListener("click", function (){
        setConfig();
    })
});

function getConfig(){
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/getConfig", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var configJSON = JSON.parse(this.responseText);
            document.getElementById("agvControlUrl").value = configJSON.agvControl.url;
            document.getElementById("agvLowBattery").value = configJSON.agv.low_battery;
            document.getElementById("agvLowBatteryDuration").value = configJSON.agv.low_battery_duration;
            document.getElementById("agvObstacleDuration").value = configJSON.agv.obstacle_duration;
            document.getElementById("httpTimeout").value = configJSON.http.timeout;
            document.getElementById("httpMaxRetry").value = configJSON.http.max_retry;
            document.getElementById("wToolslUrl").value = configJSON.jdbc.WTools.url;
            document.getElementById("wToolsUsername").value = configJSON.jdbc.WTools.username;
            document.getElementById("wToolsPassword").value = configJSON.jdbc.WTools.password;
            document.getElementById("elevatorIp").value = configJSON.elevator.ip;
            document.getElementById("elevatorPort").value = configJSON.elevator.port;
            document.getElementById("elevatorTimeout").value = configJSON.elevator.timeout;
            document.getElementById("elevatorFailSocket").value = configJSON.elevator.fail_socket;
            document.getElementById("elevatorPrePersonOpenDoorDuration").value = configJSON.elevator.pre_person_open_door_duration;
        }
    };
}

function setConfig(){
    let agvControlUrl = document.getElementById("agvControlUrl").value;
    let agvLowBattery = document.getElementById("agvLowBattery").value;
    let agvLowBatteryDuration = document.getElementById("agvLowBatteryDuration").value;
    let agvObstacleDuration = document.getElementById("agvObstacleDuration").value;
    let httpTimeout = document.getElementById("httpTimeout").value;
    let httpMaxRetry = document.getElementById("httpMaxRetry").value;
    let wToolslUrl = document.getElementById("wToolslUrl").value;
    let wToolsUsername = document.getElementById("wToolsUsername").value;
    let wToolsPassword = document.getElementById("wToolsPassword").value;
    let elevatorIp = document.getElementById("elevatorIp").value;
    let elevatorPort = document.getElementById("elevatorPort").value;
    let elevatorTimeout = document.getElementById("elevatorTimeout").value;
    let elevatorFailSocket = document.getElementById("elevatorFailSocket").value;
    let elevatorPrePersonOpenDoorDuration = document.getElementById("elevatorPrePersonOpenDoorDuration").value;
    if(agvControlUrl === ""){
        return "AgvControlUrl參數為空值";
    } else if(agvLowBattery === ""){
        return "AgvLowBattery參數為空值";
    } else if(agvLowBatteryDuration === ""){
        return "AgvLowBatteryDuration參數為空值";
    } else if(agvObstacleDuration === ""){
        return "AgvObstacleDuration參數為空值";
    } else if(httpTimeout === ""){
        return "HttpTimeout參數為空值";
    } else if(httpMaxRetry === ""){
        return "HttpMaxRetry參數為空值";
    } else if(wToolslUrl === ""){
        return "wToolslUrl參數為空值";
    } else if(wToolsUsername === ""){
        return "wToolsUsername參數為空值";
    } else if(wToolsPassword === ""){
        return "wToolsPassword參數為空值";
    } else if(elevatorIp === ""){
        return "elevatorIp參數為空值";
    } else if(elevatorPort === ""){
        return "elevatorPort參數為空值";
    } else if(elevatorTimeout === ""){
        return "elevatorTimeout參數為空值";
    } else if(elevatorFailSocket === ""){
        return "elevatorFailSocket參數為空值";
    } else if(elevatorPrePersonOpenDoorDuration === ""){
        return "elevatorPrePersonOpenDoorDuration參數為空值";
    }
    let config = {
        agvControlUrl: agvControlUrl,
        agvLowBattery: agvLowBattery,
        agvLowBatteryDuration: agvLowBatteryDuration,
        agvObstacleDuration: agvObstacleDuration,
        httpTimeout: httpTimeout,
        httpMaxRetry: httpMaxRetry,
        dbWToolsUrl: wToolslUrl,
        dbWToolsUsername: wToolsUsername,
        dbWToolsPassword: wToolsPassword,
        elevatorIp: elevatorIp,
        elevatorPort: elevatorPort,
        elevatorTimeout: elevatorTimeout,
        elevatorFailSocket: elevatorFailSocket,
        elevatorPrePersonOpenDoorDuration: elevatorPrePersonOpenDoorDuration
    };
    fetch(baseUrl+'/api/setConfig', {
        method: 'POST',  // 可以根据需要使用不同的 HTTP 方法
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(config)
    }).then(response => {
        return response.text();
    }).then(response => {
        alert(response);
    }).catch(error => {
        alert("修改失敗: ", error);
    });
}

function setGridManual(el) {
    localStorage.setItem("gridManual", el.checked?1:0);
    console.log(localStorage.getItem("gridManual"));
}