const taskList = {mode: 1, terminal: null, tasks: []};
// const taskList = {mode: 1, terminal: null, tasks: [{startGrid: 2, objectNumber: ["WCDS004-AJ-S2"]}, 
//                                         {startGrid: 3, objectNumber: ["WCDS005-AJ-S2", "WCDS005-AJ-S3"]}, 
//                                         {startGrid: 4, objectNumber: ["WCDS006-AJ-S3", "WCDS006-AJ-S4"]}]};     
var gridsStatus = null;         
var bookedGrids = [];
var occupiedGrids = [];                          

document.addEventListener("DOMContentLoaded", function() {
    bindBeforeContent();
    bindStartGridDiv();
    bindInputOption();
    var btnGroup = document.querySelector('#modeOption');
    btnGroup.addEventListener("click", function (event) {
        // 检查是否点击了选项按钮（btn-check 类的元素）
        if (event.target.classList.contains("btn-check")) {
            // 弹出确认对话框
            // const taskLength = document.querySelectorAll(".list-task").length;
            clearInput();
            if(taskList.tasks.length>0){
                var confirmResult = confirm("任務清單將被清除，確定要切換模式嗎？");
                // 如果用户点击了确认按钮，则执行切换操作
                if (!confirmResult) {
                    // 阻止选项按钮切换
                    event.preventDefault();
                    // 取消选中当前选项按钮
                    event.target.checked = true;
                } else {
                    document.getElementById("taskList").innerHTML = "";
                    taskList.tasks = [];
                    taskList.terminal = null;
                    switchPage(event.target);
                }
            } else {
                switchPage(event.target);
            }
        }
    });
    const lastMode = localStorage.getItem("mode");
    if(lastMode){
        document.getElementById(lastMode).click();
    }
    
    updateGridStatus();  //  取得狀態資料
    setInterval(updateGridStatus, 2000);  //  每秒更新
});

function updateGridStatus() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/grid/status", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var gridStatus = JSON.parse(this.responseText);
            gridUpdate(gridStatus);
        }
    };
}

function gridUpdate(data){
    gridsStatus = data;
    bookedGrids = [];
    data.forEach(function(gdata) {
        if(gdata.status === 1){
            bookedGrids.push(gdata.station);
        }
    });
    allGrids = document.querySelectorAll("[data-val]");
    allGrids.forEach(function(grid) {
        grid.classList.remove("booked");
        grid.classList.remove("occupied");
        gridName = grid.getAttribute("data-val");
        data.forEach(function(gdata) {
            if(gdata.station === gridName){
                if(gdata.status === 1){
                    grid.classList.add("booked");
                } else if(gdata.status === 2){
                    grid.classList.add("occupied");
                }
            }
        });
    });
}

function sendJSON(){
    if(taskList.terminal===null){
        alert("未選擇終點區域");
        return;
    }
    fetch(baseUrl+'/api/sendtasklist', {
        method: 'POST',  // 可以根据需要使用不同的 HTTP 方法
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(taskList)
    }).then(response => {
        return response.text();
    }).then(response => {
        // 处理后端的响应
        alert(response);
    }).catch(error => {
        // 处理错误
        alert("任務發送失敗: ", error);
    });
}

function addBtn(){

    var startGridVal = document.getElementById("startGridVal").value;
    if(startGridVal===""){
        alert("未選擇起始格位");
        return;
    }
    
    // var objectNumberDOM = document.getElementById("workNumberVals");
    // var objectNumberVals = objectNumberDOM.querySelectorAll("input");
    // var objectNumbers = [];
    // var validObjn = 0;
    // objectNumberVals.forEach(function(objectNumber){
    //     if(objectNumber.value !== ""){
    //         objectNumbers.push(objectNumber.value);
    //         validObjn++;
    //     }
    // });

    // if(validObjn===0){
    //     alert("未輸入工單號碼");
    //     return;
    // }
    var workNumbers = [];
    var lineCodes = [];
    for(let i=1;i<=4;i++){
        const workNumber = document.getElementById("workNumber"+i).value;
        if(workNumber !== ""){
            let lineCode = document.getElementById("lineCode"+i).value;
            if(lineCode === ""){
                lineCode = "00411";
            }
            workNumbers.push(workNumber);
            lineCodes.push(lineCode);
        }
    }

    taskList.tasks.push({
        startGrid: startGridVal,
        lineCode: lineCodes,
        workNumber: workNumbers
    });
    clearInput();
    refreshTask();
}

function clearInput(){
    document.getElementById("startGridVal").value = "";
    document.getElementById("workNumber1").value = "";
    document.getElementById("workNumber2").value = "";
    document.getElementById("workNumber3").value = "";
    document.getElementById("workNumber4").value = "";
    document.getElementById("lineCode1").value = "";
    document.getElementById("lineCode2").value = "";
    document.getElementById("lineCode3").value = "";
    document.getElementById("lineCode4").value = "";
}

function showJSON(){
    console.log(taskList);
}

function refreshTask(){
    var taskHTML = '';
    taskList.tasks.forEach(function(task) {
        var informations = '';
        task.workNumber.forEach(function(objn, index) {
            if(index !== task.workNumber.length-1){
                informations+= "(" + task.lineCode[index] + ")" + objn+'/';
            } else {
                informations+= "(" + task.lineCode[index] + ")" + objn;
            }
            
        });
        // informations = "(00411)MO02-20231002001/(00431)MO02-20231002001/(00431)MO02-20231002001/(01320)MO02-20231002001";
        taskHTML += `<div class="row list-task">
                        <div class="col-2 center">Start Grid: ${task.startGrid}</div>
                        <div class="col-8 center" style="overflow-x: auto;"><nobr style="width: 100%;">Information: `+ informations +`</nobr></div>
                        <div class="col-2 center">
                        <button type="button" class="btn btn-danger btn-remove" onclick="remove('${task.startGrid}')">Remove</button>
                        </div>
                    </div>`;
    });
    document.getElementById("taskList").innerHTML = taskHTML;
}

function remove(startGrid) {
    // 使用 filter 方法過濾 tasks 陣列，只保留不等於指定 startGrid 的任務
    taskList.tasks = taskList.tasks.filter(task => task.startGrid !== startGrid);
    refreshTask();
}

function bindInputOption() {
    var allOption = document.querySelectorAll(".form-select");
    allOption.forEach(function(option) {
        option.addEventListener("change", function() {
            if(option.value !== ""){
                let n = option.getAttribute("id").slice(8);
                document.getElementById("workNumber"+n).focus();
            }
        });
    });
}

function bindStartGridDiv(){
    var startDiv = document.getElementById("start");
    var startGridDivs = startDiv.querySelectorAll("td");

    startGridDivs.forEach(function(grid) {
        grid.addEventListener("click", function(event) {
            const startGridValues = taskList.tasks.map(function(task) {
                return task.startGrid;
            });
            var choosed = false;
            startGridDivs.forEach(function(otherGrid) {
                if(otherGrid === grid && startGridValues.includes(otherGrid.getAttribute("data-val"))){
                    choosed = true;
                }
            });
            if(choosed){
                alert("格位已被選定");
                return;
            }

            var booked = false;
            var gridName = grid.getAttribute("data-val");
            bookedGrids.forEach(function(bookedGrid) {
                if(bookedGrid === gridName){
                    booked = true;
                }
            });
            if(booked){
                alert("格位已被預定");
                return;
            }

            startGridDivs.forEach(function(otherGrid) {
                if(!startGridValues.includes(otherGrid.getAttribute("data-val"))) {
                    // console.log("move: ", otherGrid.getAttribute("data-val"));
                    otherGrid.classList.remove("mark");
                }
            });
            grid.classList.add("mark");
            document.getElementById("startGridVal").value = grid.getAttribute("data-val");
            
        });
    });
}

function bindBeforeContent() {
    var gridTables = document.querySelectorAll(".grid-table");
    gridTables.forEach(function(table) {
        table.addEventListener("click", function(event) {
            var style = document.createElement('style');
            style.innerHTML = '#' + table.id + '::before { color: rgba(255, 0, 0, 0.8)!important; }';
            table.style='border: 5px solid red;';
            table.appendChild(style);
            taskList.terminal = table.getAttribute('data-val');

            // 重置其他表格的::before伪元素颜色为黑色
            gridTables.forEach(function(otherTable) {
                if (otherTable.id !== table.id) {
                    var style = document.createElement('style');
                    style.innerHTML = '#' + otherTable.id + '::before { color: rgba(0, 0, 0, 0.4)!important; }';
                    otherTable.style='border: 0px;';
                    otherTable.appendChild(style);
                }
            });
        });
    });
}

function switchPage(radioButton) {
    localStorage.setItem("mode", radioButton.id);
    switch (radioButton.id) {
        case "sendMode1":
            console.log("Mode 1");
            taskList.mode=1;
            document.getElementById("startGrid").innerHTML = `<div class="col start-grid">
                                                                <div class="row start-grid-row">
                                                                <div class="col-1">
                                                                    <div class="row">
                                                                    <div class="col">
                                                                        <label  class="grid-title">1F</label>
                                                                    </div>
                                                                    </div>
                                                                </div>
                                                                <div class="col-10 flex-wrap table-container grid-btns">
                                                                    <table class="grid-table-start" id="start">
                                                                    <tbody>
                                                                        <tr>
                                                                        <td data-val="1-T-2" class="grid-btn start">2</td>
                                                                        <td data-val="1-T-4" class="grid-btn start">4</td>
                                                                        <td data-val="1-T-6" class="grid-btn start">6</td>
                                                                        <td data-val="1-T-8" class="grid-btn start">8</td>
                                                                        </tr>
                                                                        <tr>
                                                                        <td data-val="1-T-1" class="grid-btn start">1</td>
                                                                        <td data-val="1-T-3" class="grid-btn start">3</td>
                                                                        <td data-val="1-T-5" class="grid-btn start">5</td>
                                                                        <td data-val="1-T-7" class="grid-btn start">7</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                                </div>
                                                                <div class="col-1"></div>
                                                                </div>
                                                            </div>`;
            document.getElementById("terminalGrid").innerHTML = `<div class="col terminal-grid">
                                                                    <div class="row terminal-grid-row">
                                                                    <div class="col-1">
                                                                        <div class="row">
                                                                        <div class="col">
                                                                            <label class="grid-title">3F</label>
                                                                        </div>
                                                                        </div>
                                                                    </div>
                                                                    <div class="col-10 flex-wrap table-container grid-btns">
                                                                        <table class="grid-table" id="btns1" data-val="A">
                                                                        <tbody>
                                                                            <tr>
                                                                                <td data-val="3-A-2" class="grid-btn terminal">2</td>
                                                                                <td data-val="3-A-4" class="grid-btn terminal">4</td>
                                                                                <td data-val="3-A-6" class="grid-btn terminal">6</td>
                                                                                <td data-val="3-A-8" class="grid-btn terminal">8</td>
                                                                            </tr>
                                                                            <tr>
                                                                                <td data-val="3-A-1" class="grid-btn terminal">1</td>
                                                                                <td data-val="3-A-3" class="grid-btn terminal">3</td>
                                                                                <td data-val="3-A-5" class="grid-btn terminal">5</td>
                                                                                <td data-val="3-A-7" class="grid-btn terminal">7</td>
                                                                            </tr>
                                                                        </tbody>
                                                                        </table>
                                                            
                                                                        <table class="grid-table" id="btns2" data-val="B">
                                                                        <tbody>
                                                                            <tr>
                                                                            <td data-val="3-B-2" class="grid-btn terminal">2</td>
                                                                            <td data-val="3-B-4" class="grid-btn terminal">4</td>
                                                                            <td data-val="3-B-6" class="grid-btn terminal">6</td>
                                                                            <td data-val="3-B-8" class="grid-btn terminal">8</td>
                                                                            </tr>
                                                                            <tr>
                                                                            <td data-val="3-B-1" class="grid-btn terminal">1</td>
                                                                            <td data-val="3-B-3" class="grid-btn terminal">3</td>
                                                                            <td data-val="3-B-5" class="grid-btn terminal">5</td>
                                                                            <td data-val="3-B-7" class="grid-btn terminal">7</td>
                                                                            </tr>
                                                                        </tbody>
                                                                        </table>
                                                            
                                                                        <table class="grid-table" id="btns3" data-val="C">
                                                                        <tbody>
                                                                            <tr>
                                                                            <td data-val="3-C-3" class="grid-btn terminal">3</td>
                                                                            <td data-val="3-C-6" class="grid-btn terminal">6</td>
                                                                            <td data-val="3-C-9" class="grid-btn terminal">9</td>
                                                                            <td data-val="3-C-12" class="grid-btn terminal">12</td>
                                                                            <td data-val="3-C-15" class="grid-btn terminal">15</td>
                                                                            <td data-val="3-C-18" class="grid-btn terminal">18</td>
                                                                            <td data-val="3-C-21" class="grid-btn terminal">21</td>
                                                                            <td data-val="3-C-24" class="grid-btn terminal">24</td>
                                                                            </tr>
                                                                            <tr>
                                                                            <td data-val="3-C-2" class="grid-btn terminal">2</td>
                                                                            <td data-val="3-C-5" class="grid-btn terminal">5</td>
                                                                            <td data-val="3-C-8" class="grid-btn terminal">8</td>
                                                                            <td data-val="3-C-11" class="grid-btn terminal">11</td>
                                                                            <td data-val="3-C-14" class="grid-btn terminal">14</td>
                                                                            <td data-val="3-C-17" class="grid-btn terminal">17</td>
                                                                            <td data-val="3-C-20" class="grid-btn terminal">20</td>
                                                                            <td data-val="3-C-23" class="grid-btn terminal">23</td>
                                                                            </tr>
                                                                            <tr>
                                                                            <td data-val="3-C-1" class="grid-btn terminal">1</td>
                                                                            <td data-val="3-C-4" class="grid-btn terminal">4</td>
                                                                            <td data-val="3-C-7" class="grid-btn terminal">7</td>
                                                                            <td data-val="3-C-10" class="grid-btn terminal">10</td>
                                                                            <td data-val="3-C-13" class="grid-btn terminal">13</td>
                                                                            <td data-val="3-C-16" class="grid-btn terminal">16</td>
                                                                            <td data-val="3-C-19" class="grid-btn terminal">19</td>
                                                                            <td data-val="3-C-22" class="grid-btn terminal">22</td>
                                                                            </tr>
                                                                        </tbody>
                                                                        </table>
                                                                        <table class="grid-table" id="btns4" data-val="D">
                                                                        <tbody>
                                                                            <tr>
                                                                                <td data-val="3-D-2" class="grid-btn terminal">2</td>
                                                                                <td data-val="3-D-4" class="grid-btn terminal">4</td>
                                                                                <td data-val="3-D-6" class="grid-btn terminal">6</td>
                                                                                <td data-val="3-D-8" class="grid-btn terminal">8</td>
                                                                            </tr>
                                                                            <tr>
                                                                                <td data-val="3-D-1" class="grid-btn terminal">1</td>
                                                                                <td data-val="3-D-3" class="grid-btn terminal">3</td>
                                                                                <td data-val="3-D-5" class="grid-btn terminal">5</td>
                                                                                <td data-val="3-D-7" class="grid-btn terminal">7</td>
                                                                            </tr>
                                                                        </tbody>
                                                                        </table>
                                                                    </div>
                                                                    <div class="col-1"></div>
                                                                    </div>
                                                                </div>`;
            break;
        case "sendMode2":
            console.log("Mode 2");
            taskList.mode=2;
            document.getElementById("startGrid").innerHTML = `<div class="col start-grid">
                                                                <div class="row start-grid-row">
                                                                <div class="col-1">
                                                                    <div class="row">
                                                                    <div class="col">
                                                                        <label  class="grid-title">2F</label>
                                                                    </div>
                                                                    </div>
                                                                </div>
                                                                <div class="col-10 flex-wrap table-container grid-btns">
                                                                    <table class="grid-table-start" id="start">
                                                                    <tbody>
                                                                        <tr>
                                                                        <td data-val="2-T-10" class="grid-btn start">10</td>
                                                                        <td data-val="2-T-8" class="grid-btn start">8</td>
                                                                        <td data-val="2-T-6" class="grid-btn start">6</td>
                                                                        <td data-val="2-T-4" class="grid-btn start">4</td>
                                                                        <td data-val="2-T-2" class="grid-btn start">2</td>
                                                                        </tr>
                                                                        <tr>
                                                                        <td data-val="2-T-9" class="grid-btn start">9</td>
                                                                        <td data-val="2-T-7" class="grid-btn start">7</td>
                                                                        <td data-val="2-T-5" class="grid-btn start">5</td>
                                                                        <td data-val="2-T-3" class="grid-btn start">3</td>
                                                                        <td data-val="2-T-1" class="grid-btn start">1</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                                </div>
                                                                <div class="col-1"></div>
                                                                </div>
                                                            </div>`;
            document.getElementById("terminalGrid").innerHTML = `<div class="col terminal-grid">
                                                                    <div class="row terminal-grid-row">
                                                                        <div class="col-1">
                                                                            <div class="row">
                                                                                <div class="col">
                                                                                    <label class="grid-title">2F</label>
                                                                                </div>
                                                                            </div>
                                                                        </div>
                                                                        <div class="col-10 flex-wrap table-container grid-btns">
                                                                            <table class="grid-table" id="btns1" data-val="A">
                                                                                <tbody>
                                                                                    <tr>
                                                                                        <td data-val="2-A-19" class="grid-btn terminal">19</td>
                                                                                        <td data-val="2-A-17" class="grid-btn terminal">17</td>
                                                                                        <td data-val="2-A-15" class="grid-btn terminal">15</td>
                                                                                        <td data-val="2-A-13" class="grid-btn terminal">13</td>
                                                                                        <td data-val="2-A-11" class="grid-btn terminal">11</td>
                                                                                        <td data-val="2-A-9" class="grid-btn terminal">9</td>
                                                                                        <td data-val="2-A-7" class="grid-btn terminal">7</td>
                                                                                        <td data-val="2-A-5" class="grid-btn terminal">5</td>
                                                                                        <td data-val="2-A-3" class="grid-btn terminal">3</td>
                                                                                        <td data-val="2-A-1" class="grid-btn terminal">1</td>
                                                                                    </tr>
                                                                                    <tr>
                                                                                        <td data-val="2-A-20" class="grid-btn terminal">20</td>
                                                                                        <td data-val="2-A-18" class="grid-btn terminal">18</td>
                                                                                        <td data-val="2-A-16" class="grid-btn terminal">16</td>
                                                                                        <td data-val="2-A-14" class="grid-btn terminal">14</td>
                                                                                        <td data-val="2-A-12" class="grid-btn terminal">12</td>
                                                                                        <td data-val="2-A-10" class="grid-btn terminal">10</td>
                                                                                        <td data-val="2-A-8" class="grid-btn terminal">8</td>
                                                                                        <td data-val="2-A-6" class="grid-btn terminal">6</td>
                                                                                        <td data-val="2-A-4" class="grid-btn terminal">4</td>
                                                                                        <td data-val="2-A-2" class="grid-btn terminal">2</td>
                                                                                    </tr>
                                                                                </tbody>
                                                                            </table>
                                                                        </div>
                                                                        <div class="col-1"></div>
                                                                    </div>
                                                                </div>`;
            break;
        case "sendMode3":
            console.log("Mode 3");
            taskList.mode=3;
            document.getElementById("startGrid").innerHTML = `<div class="col start-grid">
                                                                <div class="row start-grid-row">
                                                                <div class="col-1">
                                                                    <div class="row">
                                                                    <div class="col">
                                                                        <label  class="grid-title">3F</label>
                                                                    </div>
                                                                    </div>
                                                                </div>
                                                                <div class="col-10 flex-wrap table-container grid-btns" id="start">
                                                                    <table class="grid-table-start">
                                                                    <tbody>
                                                                        <tr>
                                                                        <td data-val="3-R-1" class="grid-btn start">1</td>
                                                                        <td data-val="3-R-2" class="grid-btn start">2</td>
                                                                        <td data-val="3-R-3" class="grid-btn start">3</td>
                                                                        <td data-val="3-R-4" class="grid-btn start">4</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                                    <table class="grid-table-start">
                                                                    <tbody>
                                                                        <tr>
                                                                        <td data-val="3-R-5" class="grid-btn start">5</td>
                                                                        <td data-val="3-R-6" class="grid-btn start">6</td>
                                                                        <td data-val="3-R-7" class="grid-btn start">7</td>
                                                                        <td data-val="3-R-8" class="grid-btn start">8</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                                </div>
                                                                <div class="col-1"></div>
                                                                </div>
                                                            </div>`;
            document.getElementById("terminalGrid").innerHTML = `<div class="col terminal-grid">
                                                                    <div class="row terminal-grid-row">
                                                                        <div class="col-1">
                                                                            <div class="row">
                                                                                <div class="col">
                                                                                    <label class="grid-title">1F</label>
                                                                                </div>
                                                                            </div>
                                                                        </div>
                                                                        <div class="col-10 flex-wrap table-container grid-btns">
                                                                            <table class="grid-table" id="btns9" data-val="R">
                                                                                <tbody>
                                                                                    <tr>
                                                                                        <td data-val="1-R-1" class="grid-btn terminal">1</td>
                                                                                        <td data-val="1-R-1" class="grid-btn terminal">2</td>
                                                                                    </tr>
                                                                                </tbody>
                                                                            </table>
                                                                        </div>
                                                                        <div class="col-1"></div>
                                                                    </div>
                                                                </div>`;            
            
            break;
        default:
            console.log("Mode error");
            break;
    }
    bindStartGridDiv();
    bindBeforeContent();
    updateGridStatus();
}