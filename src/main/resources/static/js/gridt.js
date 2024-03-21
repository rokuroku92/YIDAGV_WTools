
document.addEventListener("DOMContentLoaded", function() {
    if(localStorage.getItem("gridManual") == 1){
        document.getElementById("logo").style.fill = "#FF0000";
    }

    var modeBtn = document.querySelector('#modeOption');
    modeBtn.addEventListener("click", function (event) {
        // 检查是否点击了选项按钮（btn-check 类的元素）
        if (event.target.classList.contains("btn-check")) {
            switchPage(event.target);
        }
    });

    var showBtn = document.querySelector('#showOption');
    showBtn.addEventListener("click", function (event) {
        const el = event.target;
        const block = document.getElementById("gridBlock");
        const list = document.getElementById("gridList");
        if(el.classList.contains("active")){
            // console.log("yes");
            el.innerHTML = '<svg class="bi" width="24" height="24" role="img"><use xlink:href="#return"/></svg>';
            
            block.style.display = "none";
            list.style.display = "block";
        } else {
            // console.log("no");
            el.innerHTML = '<svg class="bi" width="24" height="24" role="img"><use xlink:href="#list"/></svg>';

            block.style.display = "block";
            list.style.display = "none";
        }
    });
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

const lineCodeOption = {
    "00411": "棘輪一線(00411)", 
    "00412": "棘輪二線(00412)",
    "00413": "棘輪三線(00413)",
    "00414": "棘輪四線(00414)",
    "00431": "扭力校正(00431)",
    "00436": "扭力組立(00436)",
    "00446": "雷射加工(00446)",
    "00451": "備工線(00451)",
    "01320": "秀山加工(01320)"
};
var lastGridListHTML;
function gridUpdate(data){
    allGrids = document.querySelectorAll("[data-val]");
    var gridListHTML = "";
    allGrids.forEach(function(grid) {
        grid.classList.remove("booked");
        grid.classList.remove("occupied");
        grid.removeAttribute("onclick");
        gridName = grid.getAttribute("data-val");
        data.forEach(function(gdata) {
            if(gdata.station === gridName){
                var status = "Free";
                if(gdata.status === 1){
                    grid.classList.add("booked");
                    status = "Booked";
                    if(gdata.lineCode_1 != null){
                        let newLineCode1 = `<select onchange="updateLineCode('${gridName}', '1', this)" class="form-select">`
                        Object.entries(lineCodeOption).forEach(([lineCode, name]) => {
                            if(gdata.lineCode_1 === lineCode){
                                newLineCode1 += `<option selected value="${lineCode}">${name}</option>`;
                            } else {
                                newLineCode1 += `<option value="${lineCode}">${name}</option>`;
                            }
                        });
                        newLineCode1 += `</select>`;
                        gdata.lineCode_1 = newLineCode1;
                    }
                    if(gdata.lineCode_2 != null){
                        let newLineCode2 = `<select onchange="updateLineCode('${gridName}', '2', this)" class="form-select">`
                        Object.entries(lineCodeOption).forEach(([lineCode, name]) => {
                            if(gdata.lineCode_2 === lineCode){
                                newLineCode2 += `<option selected value="${lineCode}">${name}</option>`;
                            } else {
                                newLineCode2 += `<option value="${lineCode}">${name}</option>`;
                            }
                        });
                        newLineCode2 += `</select>`;
                        gdata.lineCode_2 = newLineCode2;
                    }
                    if(gdata.lineCode_3 != null){
                        let newLineCode3 = `<select onchange="updateLineCode('${gridName}', '3', this)" class="form-select">`
                        Object.entries(lineCodeOption).forEach(([lineCode, name]) => {
                            if(gdata.lineCode_3 === lineCode){
                                newLineCode3 += `<option selected value="${lineCode}">${name}</option>`;
                            } else {
                                newLineCode3 += `<option value="${lineCode}">${name}</option>`;
                            }
                        });
                        newLineCode3 += `</select>`;
                        gdata.lineCode_3 = newLineCode3;
                    }
                    if(gdata.lineCode_4 != null){
                        let newLineCode4 = `<select onchange="updateLineCode('${gridName}', '4', this)" class="form-select">`
                        Object.entries(lineCodeOption).forEach(([lineCode, name]) => {
                            if(gdata.lineCode_4 === lineCode){
                                newLineCode4 += `<option selected value="${lineCode}">${name}</option>`;
                            } else {
                                newLineCode4 += `<option value="${lineCode}">${name}</option>`;
                            }
                        });
                        newLineCode4 += `</select>`;
                        gdata.lineCode_4 = newLineCode4;
                    }
                    
                } else if(gdata.status === 2){
                    grid.classList.add("occupied");
                    status = "Occupied";
                    grid.setAttribute("onclick", "clearGrid('" + gridName + "')");
                } else if(gdata.status === 0){
                    grid.setAttribute("onclick", "occupiedGrid('" + gridName + "')");
                }
                if(gdata.workNumber_1 != null){
                    gridListHTML += `<tr>
                                        <th>` + gdata.station + `</td>
                                        <td>` + status + `</td>
                                        <td>` + gdata.workNumber_1 + `</td>
                                        <td>` + gdata.objectName_1 + `</td>
                                        <td>` + gdata.objectNumber_1 + `</td>
                                        <td>` + gdata.lineCode_1 + `</td>
                                    </tr>`;
                }
                if(gdata.workNumber_2 != null){
                    gridListHTML += `<tr>
                                        <th>` + gdata.station + `</td>
                                        <td>` + status + `</td>
                                        <td>` + gdata.workNumber_2 + `</td>
                                        <td>` + gdata.objectName_2 + `</td>
                                        <td>` + gdata.objectNumber_2 + `</td>
                                        <td>` + gdata.lineCode_2 + `</td>
                                    </tr>`;
                }
                if(gdata.workNumber_3 != null){
                    gridListHTML += `<tr>
                                        <th>` + gdata.station + `</td>
                                        <td>` + status + `</td>
                                        <td>` + gdata.workNumber_3 + `</td>
                                        <td>` + gdata.objectName_3 + `</td>
                                        <td>` + gdata.objectNumber_3 + `</td>
                                        <td>` + gdata.lineCode_3 + `</td>
                                    </tr>`;
                }
                if(gdata.workNumber_4 != null){
                    gridListHTML += `<tr>
                                        <th>` + gdata.station + `</td>
                                        <td>` + status + `</td>
                                        <td>` + gdata.workNumber_4 + `</td>
                                        <td>` + gdata.objectName_4 + `</td>
                                        <td>` + gdata.objectNumber_4 + `</td>
                                        <td>` + gdata.lineCode_4 + `</td>
                                    </tr>`;
                }
            }
        });
    });
    if(lastGridListHTML != gridListHTML){
        document.getElementById("gridListContent").innerHTML = gridListHTML;
    }
    lastGridListHTML = gridListHTML;
}

function updateLineCode(gridName, lineNumber, el){
    fetch(baseUrl + `/api/grid/updateLineCode?gridName=${gridName}&lineNumber=${lineNumber}&lineCode=${el.value}`)
    .then(response => {
        if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
        }

        return response.text();
    })
    .then(data => {
        if(data != "OK"){
            alert(data);
        } else {
            console.log(data);
        }
    })
    .catch(error => {
        console.error('Fetch error:', error);
    });

}

function clearGrid(gridName){
    var result = confirm("是否要取消 " + gridName + " 的鎖定");
    if(result){
        fetch(baseUrl + `/api/grid/clearGrid?gridName=${gridName}`)
        .then(response => {
            if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.text();
        })
        .then(data => {
            if(data != "OK"){
                alert(data);
            } else {
                console.log(data);
            }
        })
        .catch(error => {
            console.error('Fetch error:', error);
        });
    }
}

function occupiedGrid(gridName){
    if(localStorage.getItem("gridManual") != 1){
        return;
    }
    let result = confirm("是否要將 " + gridName + " 設為佔用");
    if(result){
        fetch(baseUrl + `/api/grid/occupiedGrid?gridName=${gridName}`)
        .then(response => {
            if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.text();
        })
        .then(data => {
            if(data != "OK"){
                alert(data);
            } else {
                console.log(data);
            }
        })
        .catch(error => {
            console.error('Fetch error:', error);
        });
    }
}

function switchPage(radioButton) {
    switch (radioButton.id) {
        case "mode1":
            console.log("Mode 1");
            document.getElementById("gridBlock").innerHTML = `<div class="col grid">
                                                                <div class="row grid-row">
                                                                <div class="col flex-wrap table-container grid-btns">
                                                                    <table class="grid-table" id="btns9">
                                                                    <tbody>
                                                                        <tr>
                                                                            <td data-val="1-R-1" class="grid-btn">1</td>
                                                                            <td data-val="1-R-2" class="grid-btn">2</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                                </div>
                                                                </div>
                                                            </div>`;
            // document.getElementById("gridListContent").innerHTML = `<tr>
            //                                                             <th>A-1</td>
            //                                                             <td>Occupied</td>
            //                                                             <td>WCDS003-AJ-S2</td>
            //                                                             <td>WCDS004-AJ-S2</td>
            //                                                             <td></td>
            //                                                             <td></td>
            //                                                         </tr>

            //                                                         <tr>
            //                                                         <th>A-2</td>
            //                                                         <td>Occupied</td>
            //                                                         <td>WCDS003-AJ-S2</td>
            //                                                         <td>WCDS004-AJ-S2</td>
            //                                                         <td>WCDS005-AJ-S2</td>
            //                                                         <td>WCDS006-AJ-S2</td>
            //                                                         </tr>`;
            break;
        case "mode2":
            console.log("Mode 2");
            document.getElementById("gridBlock").innerHTML = `<div class="col grid">
                                                                <div class="row grid-row">
                                                                    <div class="col flex-wrap table-container grid-btns">
                                                                    <table class="grid-table" id="btns1">
                                                                        <tbody>
                                                                        <tr>
                                                                            <td data-val="2-A-19" class="grid-btn">19</td>
                                                                            <td data-val="2-A-17" class="grid-btn">17</td>
                                                                            <td data-val="2-A-15" class="grid-btn">15</td>
                                                                            <td data-val="2-A-13" class="grid-btn">13</td>
                                                                            <td data-val="2-A-11" class="grid-btn">11</td>
                                                                            <td data-val="2-A-9" class="grid-btn">9</td>
                                                                            <td data-val="2-A-7" class="grid-btn">7</td>
                                                                            <td data-val="2-A-5" class="grid-btn">5</td>
                                                                            <td data-val="2-A-3" class="grid-btn">3</td>
                                                                            <td data-val="2-A-1" class="grid-btn">1</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td data-val="2-A-20" class="grid-btn">20</td>
                                                                            <td data-val="2-A-18" class="grid-btn">18</td>
                                                                            <td data-val="2-A-16" class="grid-btn">16</td>
                                                                            <td data-val="2-A-14" class="grid-btn">14</td>
                                                                            <td data-val="2-A-12" class="grid-btn">12</td>
                                                                            <td data-val="2-A-10" class="grid-btn">10</td>
                                                                            <td data-val="2-A-8" class="grid-btn">8</td>
                                                                            <td data-val="2-A-6" class="grid-btn">6</td>
                                                                            <td data-val="2-A-4" class="grid-btn">4</td>
                                                                            <td data-val="2-A-2" class="grid-btn">2</td>
                                                                        </tr>
                                                                        </tbody>
                                                                    </table>
                                                                    </div>
                                                                </div>
                                                            </div>`;
            break;
        case "mode3":
            console.log("Mode 3");
            document.getElementById("gridBlock").innerHTML = `<div class="col grid">
                                                                <div class="row grid-row">
                                                                <div class="col flex-wrap table-container grid-btns">
                                                                    <table class="grid-table" id="btns1">
                                                                    <tbody>
                                                                        <tr>
                                                                            <td data-val="3-A-2" class="grid-btn">2</td>
                                                                            <td data-val="3-A-4" class="grid-btn">4</td>
                                                                            <td data-val="3-A-6" class="grid-btn">6</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td data-val="3-A-1" class="grid-btn">1</td>
                                                                            <td data-val="3-A-3" class="grid-btn">3</td>
                                                                            <td data-val="3-A-5" class="grid-btn">5</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                        
                                                                    <table class="grid-table" id="btns2" data-val="B">
                                                                    <tbody>
                                                                        <tr>
                                                                        <td data-val="3-B-2" class="grid-btn">2</td>
                                                                        <td data-val="3-B-4" class="grid-btn">4</td>
                                                                        <td data-val="3-B-6" class="grid-btn">6</td>
                                                                        <td data-val="3-B-8" class="grid-btn">8</td>
                                                                        </tr>
                                                                        <tr>
                                                                        <td data-val="3-B-1" class="grid-btn">1</td>
                                                                        <td data-val="3-B-3" class="grid-btn">3</td>
                                                                        <td data-val="3-B-5" class="grid-btn">5</td>
                                                                        <td data-val="3-B-7" class="grid-btn">7</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                        
                                                                    <table class="grid-table" id="btns3" data-val="C">
                                                                    <tbody>
                                                                        <tr>
                                                                        <td data-val="3-C-3" class="grid-btn">3</td>
                                                                        <td data-val="3-C-6" class="grid-btn">6</td>
                                                                        <td data-val="3-C-9" class="grid-btn">9</td>
                                                                        <td data-val="3-C-12" class="grid-btn">12</td>
                                                                        <td data-val="3-C-15" class="grid-btn">15</td>
                                                                        <td data-val="3-C-18" class="grid-btn">18</td>
                                                                        </tr>
                                                                        <tr>
                                                                        <td data-val="3-C-2" class="grid-btn">2</td>
                                                                        <td data-val="3-C-5" class="grid-btn">5</td>
                                                                        <td data-val="3-C-8" class="grid-btn">8</td>
                                                                        <td data-val="3-C-11" class="grid-btn">11</td>
                                                                        <td data-val="3-C-14" class="grid-btn">14</td>
                                                                        <td data-val="3-C-17" class="grid-btn">17</td>
                                                                        </tr>
                                                                        <tr>
                                                                        <td data-val="3-C-1" class="grid-btn">1</td>
                                                                        <td data-val="3-C-4" class="grid-btn">4</td>
                                                                        <td data-val="3-C-7" class="grid-btn">7</td>
                                                                        <td data-val="3-C-10" class="grid-btn">10</td>
                                                                        <td data-val="3-C-13" class="grid-btn">13</td>
                                                                        <td data-val="3-C-16" class="grid-btn">16</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                                    <table class="grid-table" id="btns4" data-val="D">
                                                                    <tbody>
                                                                        <tr>
                                                                            <td data-val="3-D-2" class="grid-btn">2</td>
                                                                            <td data-val="3-D-4" class="grid-btn">4</td>
                                                                            <td data-val="3-D-6" class="grid-btn">6</td>
                                                                            <td data-val="3-D-8" class="grid-btn">8</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td data-val="3-D-1" class="grid-btn">1</td>
                                                                            <td data-val="3-D-3" class="grid-btn">3</td>
                                                                            <td data-val="3-D-5" class="grid-btn">5</td>
                                                                            <td data-val="3-D-7" class="grid-btn">7</td>
                                                                        </tr>
                                                                    </tbody>
                                                                    </table>
                                                                </div>
                                                                </div>
                                                            </div>`;
            break;
        default:
            console.log("Mode error");
            break;
    }
    updateGridStatus();
}