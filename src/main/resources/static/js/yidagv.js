// 選擇日期(稼動率)
$(function() {
    $('#datepicker').datepicker({
        format:"yyyy-mm-dd",
        defaultDate:new Date()
    });
});
$(function() {
    $('#datepicker1').datepicker({
        format:"yyyy-mm-dd",
        defaultDate:new Date()
    });
});
var xhr = new XMLHttpRequest();
var baseUrl = 'http://localhost:8080${pageContext.request.contextPath}/mvc';

window.onload = function(){
    setInterval(getData, 1000);
};

function getData() {
    xhr.open('GET', baseUrl + "/agv/json", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var data = JSON.parse(this.responseText);
            console.log(data);
            update(data);
        }
    };
}

function removeTaskById(id) {
    var check = confirm('是否要刪除任務：' + id + ' ?');

    if(!check) return;

    xhr.open('GET', baseUrl + "/agv/remove/task/" + id, true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var data = this.responseText;
            console.log(data);
            if(data == 'ok') {
                getData();
            }
        }
    };
}

function update(data){  // 更新資料
    // 工作狀態
    switch (data.status) {
        case 0:
          document.getElementById("status").value = "未連線";
          break;
        case 1:
          document.getElementById("status").value = "待命中";
          break;
        case 2:
          document.getElementById("status").value = "任務執行中";
          break;
        default:
          console.log(`內容錯誤: ${data.status}.`);
    }
    document.getElementById("task").value = data.task;  // 目前任務
    document.getElementById("place").value = data.place.id;  // 目前位置
    document.getElementById("battery").value = data.battery+"%";  // 目前電壓
    // 放車子
    document.getElementById("agv_car").innerHTML = '<img src=\"${pageContext.request.contextPath}/image/car.png\" width=\"80\" ' +
                                                   'style=\"position: absolute;left: ' + data.place.coordinate[0] + 'px;top: ' + data.place.coordinate[1] + 'px;z-index: 10\" />';
    // 清除佇列任務
    document.getElementById('task_body').innerHTML = '';
    // 加入佇列任務
    var tbody_html = "";
    console.log(data.tasks.length);
    for(let i=0;i<data.tasks.length;i++){
        let n = String("task"+(i));
        tbody_html += "<tr class=\"task\" id=\""+n+"\"></tr>";
        console.log(n);
    }
    document.getElementById("task_body").innerHTML = tbody_html;
    for(let i=0;i<data.tasks.length;i++){
        let n = String("task"+(i));
        document.getElementById(n).innerHTML = "<td>"+data.tasks[i].start_station+"</td><td>"+
                data.tasks[i].notice_station+"</td><td>"+data.tasks[i].end_station+"</td>"+
                "<button type=\"button\" class=\"btn btnt\" onclick=\"removeTaskById("+data.tasks[i].id+")\">"+
                "<svg xmlns=\"${pageContext.request.contextPath}/image/trash.svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-trash\" viewBox=\"0 0 16 16\">"+
                "<path d=\"M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z\"/>"+
                "<path fill-rule=\"evenodd\" d=\"M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z\"/>"+
                "</svg></button>";
    }
    // 更改站點按鈕顏色
    for(var i=1;i<4;i++){
        for(var j=1;j<6;j++){
            let n = "s" + String(i)+String(j);
            let m = String(i)+"-"+String(j);
            switch (data.station[n]) {
                case 0:
                  document.getElementById(m).className = "st btn btn-success disabled";
                  break;
                case 1:
                  document.getElementById(m).className = "st btn btn-danger disabled";
                  break;
                case 2:
                  document.getElementById(m).className = "st btn btn-primary";
                  break;
                case 3:
                  document.getElementById(m).className = "st btn btn-warning disabled";
                  break;
                default:
                  console.log(`內容錯誤: ${data.station[n]}.`);
            }
        }
    }

}

function setfromStationNo(no) {
    document.getElementById('ststation').value = no;
    var noText  = '';
    for(let i=1;i<4;i++){
        for(let j=1;j<6;j++){
            let n = 1000+i*10+j;
            if(no == n) noText = String(i+"-"+j);
        }
    }
    document.getElementById('ststationText').value = noText;
}

function setServiceNo(no) {
    document.getElementById('noticestation').value = no;
    var noText  = '';
    switch(no) {
        case 1511:
            noText = 'PCB測試';
            break;
        case 1512:
            noText = 'PCB外線';
            break;
        case 1513:
            noText = 'PCB外AOI';
            break;
        case 1514:
            noText = 'PCB網印';
            break;
        case 1515:
            noText = 'CNC二廠';
            break;
        case 1521:
            noText = 'FQC';
            break;
        case 1522:
            noText = 'BGA整面C';
            break;
        case 1523:
            noText = '棕化';
            break;
        case 1524:
            noText = '內層線路';
            break;
        case 1525:
            noText = 'Suep';
            break;
        case 1531:
            noText = 'FVI';
            break;
        case 1532:
            noText = 'PCB噴塗';
            break;
        case 1533:
            noText = 'BGA整面A';
            break;
        case 1534:
            noText = 'CNC一廠';
            break;
        case 1535:
            noText = 'Routing';
            break;
    }
    document.getElementById('noticestationText').value = noText;
}

/*
// 監聽按鈕(實作將選擇的按鈕放入確認列)
const cbtns = document.querySelectorAll(".st");
const cbtns1 = document.querySelectorAll(".st1");

function doit() {
    document.getElementById("ststation").value = this.id;
}

function doit1() {
    document.getElementById("noticestation").value = this.id;
}

cbtns.forEach(
  function(cbtn) {
   cbtn.addEventListener("click", doit,false);
  }
);

cbtns1.forEach(
  function(cbtn1) {
   cbtn1.addEventListener("click", doit1,false);
  }
);
*/
// 紀錄確認列與發送
function subm(){
//                alert("http://192.168.1.246:20100/task0=1&1&"+ststation+"&"+noticestation+"&"+ststation);
    alert("開始站: " + document.getElementById('ststation').value +", 通知站: "+ document.getElementById('noticestation').value);
};
// 清除按鈕
function cn(){
    document.getElementById("ststation").value = "";
    document.getElementById("noticestation").value = "";
}
