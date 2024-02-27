var elevatorPermission = null;

document.addEventListener("DOMContentLoaded", function() {
    const elevatorDiv = document.getElementById('elevatorDiv');
    elevatorDiv.addEventListener('click', cancelPersonOccupied);

    // var elevatorStatus = {
    //     elevatorPermission: "PERSON",
    //     elevatorPersonCount: 350,
    //     iAlarmObstacle: true,
    //     iConnected: true,
    //     iManual: false,  // 未使用
    //     iScan: false,
    //     iBuzzer: false,  // 未使用
    //     iError: false
    // };
    // elevatorUpdate(elevatorStatus);

    updateElevatorStatus();  //  取得狀態資料
    setInterval(updateElevatorStatus, 1000);  //  每秒更新
});

function cancelPersonOccupied(){
    if (elevatorPermission == "PERSON") {
        let iCancel = confirm("是否強制取消手動電梯模式？");
        if (iCancel) {
            // 送出取消佔用
            fetch(baseUrl + "/api/elevator/cancelPersonOccupied")
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.text();
            })
            .then(data => {
                if(data != "OK"){
                    alert("取消人員佔用失敗");
                }
            })
            .catch(error => {
                console.error('Fetch error:', error);
            });
        }
    }
}

function updateElevatorStatus(){
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/elevator/status", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var elevatorStatus = JSON.parse(this.responseText);
            elevatorUpdate(elevatorStatus);
        }
    };
}

function elevatorUpdate(data) {
    if (!data.iConnected) {
        document.getElementById("elevatorDiv").style.fill = null;
        document.getElementById("elevatorDiv").classList.add('active', 'error');
        document.getElementById("elevatorTime").style.display = "block";
        document.getElementById("elevatorTime").style.color = "#000000";
        document.getElementById("elevatorTime").innerHTML = "未連線";
        elevatorPermission = null;
    } else if (data.iError) {
        document.getElementById("elevatorDiv").style.fill = null;
        document.getElementById("elevatorDiv").classList.add('active', 'error');
        document.getElementById("elevatorTime").style.display = "block";
        document.getElementById("elevatorTime").style.color = "#000000";
        document.getElementById("elevatorTime").innerHTML = "電梯Error";
        elevatorPermission = null;
    } else if (data.iAlarmObstacle) {
        document.getElementById("elevatorDiv").style.fill = null;
        document.getElementById("elevatorDiv").classList.add('active', 'error');
        document.getElementById("elevatorTime").style.display = "block";
        document.getElementById("elevatorTime").style.color = "#000000";
        document.getElementById("elevatorTime").innerHTML = "請清除電梯異物";
        elevatorPermission = null;
    } else {
        elevatorPermission = data.elevatorPermission;
        switch (data.elevatorPermission) {
            case "FREE":
            case "PRE_PERSON":
                document.getElementById("elevatorDiv").style.fill = null;
                document.getElementById("elevatorDiv").classList.remove('active', 'error');
                document.getElementById("elevatorTime").style.display = "none";
                if (data.iScan) {
                    document.getElementById("elevatorTime").innerHTML = "電梯中有異物";
                }
                break;
            case "PERSON":
                let originalSecond = data.elevatorPersonCount;
                let minute = Math.floor(originalSecond/60);
                let second = originalSecond%60;
    
                document.getElementById("elevatorDiv").style.fill = null;
                if(originalSecond > 1200){
                    document.getElementById("elevatorDiv").classList.add('active', 'error');
                    document.getElementById("elevatorTime").style.display = "block";
                    document.getElementById("elevatorTime").style.color = "#000000";
                    document.getElementById("elevatorTime").innerHTML = `${minute}m${second}s`;
                } else {
                    document.getElementById("elevatorDiv").classList.remove('error');
                    document.getElementById("elevatorDiv").classList.add('active');
                    document.getElementById("elevatorTime").style.display = "block";
                    document.getElementById("elevatorTime").style.color = "#000000";
                    document.getElementById("elevatorTime").innerHTML = `${minute}m${second}s`;
                }
                break;
            case "SYSTEM":
                document.getElementById("elevatorDiv").style.fill = "#EAC343";
                document.getElementById("elevatorTime").style.display = "block";
                document.getElementById("elevatorTime").style.color = "#EAC343";
                if (data.iScan) {
                    document.getElementById("elevatorTime").innerHTML = "搬運中";
                } else {
                    document.getElementById("elevatorTime").innerHTML = "系統佔用";
                }
                break;
            default:
                document.getElementById("elevatorDiv").style.fill = "#FF0000";
                document.getElementById("elevatorDiv").classList.remove('active', 'error');
                document.getElementById("elevatorTime").style.display = "block";
                document.getElementById("elevatorTime").style.color = "#FF0000";
                document.getElementById("elevatorTime").innerHTML = "電梯狀態錯誤";
                console.error("電梯狀態錯誤");
                console.error(data.elevatorPermission);
                break;
        }
    }
    
}