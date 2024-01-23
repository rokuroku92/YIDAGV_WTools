
Chart.register(ChartDataLabels); // 注册插件

var option;
var myChart = null;
var myChart1 = null;

document.addEventListener("DOMContentLoaded", function() {
    init();
});

function getStatusData() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/agv/json", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var dataStatus = JSON.parse(this.responseText);
//            console.log(dataStatus);
            update(dataStatus);
        }
    };
}
function update(dataStatus){  // 更新資料

}
async function init(){
    // await setAGVBtn();
    ctx = document.getElementById("myChart");
    ctx1 = document.getElementById("workNumberChart");


    const getOrCreateLegendList = (chart, id) => {
        const legendContainer = document.getElementById(id);
        legendContainer.style.marginTop = '10px';
        let listContainer = legendContainer.querySelector('ul');
      
        if (!listContainer) {
          listContainer = document.createElement('ul');
          listContainer.style.display = 'flex';
          listContainer.style.flexDirection = 'row';
          listContainer.style.margin = 0;
          listContainer.style.padding = 0;
      
          legendContainer.appendChild(listContainer);
        }
      
        return listContainer;
      };

    const htmlLegendPlugin = {
        id: 'htmlLegend',
        afterUpdate(chart, args, options) {
          const ul = getOrCreateLegendList(chart, options.containerID);
      
          // Remove old legend items
          while (ul.firstChild) {
            ul.firstChild.remove();
          }
      
          // Reuse the built-in legendItems generator
          const items = chart.options.plugins.legend.labels.generateLabels(chart);
      
          items.forEach(item => {
            const li = document.createElement('li');
            li.style.alignItems = 'center';
            li.style.cursor = 'pointer';
            li.style.display = 'flex';
            li.style.flexDirection = 'row';
            li.style.marginLeft = '10px';
      
            li.onclick = () => {
              const {type} = chart.config;
              if (type === 'pie' || type === 'doughnut') {
                // Pie and doughnut charts only have a single dataset and visibility is per item
                chart.toggleDataVisibility(item.index);
              } else {
                chart.setDatasetVisibility(item.datasetIndex, !chart.isDatasetVisible(item.datasetIndex));
              }
              chart.update();
            };
      
            // Color box
            const boxSpan = document.createElement('span');
            // boxSpan.style.background = item.fillStyle;
            boxSpan.style.background = item.strokeStyle;
            boxSpan.style.borderColor = item.strokeStyle;
            boxSpan.style.borderWidth = item.lineWidth + 'px';
            boxSpan.style.display = 'inline-block';
            boxSpan.style.height = '20px';
            boxSpan.style.marginRight = '10px';
            boxSpan.style.width = '20px';
      
            // Text
            const textContainer = document.createElement('p');
            textContainer.style.color = item.fontColor;
            textContainer.style.margin = 0;
            textContainer.style.padding = 0;
            textContainer.style.textDecoration = item.hidden ? 'line-through' : '';
      
            const text = document.createTextNode(item.text);
            textContainer.appendChild(text);
      
            li.appendChild(boxSpan);
            li.appendChild(textContainer);
            ul.appendChild(li);
          });
        }
    };

    myChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [
                
            ],
            datasets: [{
                label:"稼動率",
                data: [
                
                ],
                lineTension: 0,
                backgroundColor: 'transparent',
                borderColor: '#007bff',
                borderWidth: 3,
                pointBackgroundColor: '#007bff'
            },
            {
                label:"任務數",
                data: [
                    
                ],
                lineTension: 0,
                backgroundColor: 'transparent',
                borderColor: '#FFB957',
                borderWidth: 3,
                pointBackgroundColor: '#FFB957'
                }]
        },
        options: {
            plugins: {
                datalabels: {
                    display: false,
                },
                htmlLegend: {
                    containerID: 'legend-container'
                },
                legend: {
                    display: false,
                    labels: {
                        color: '#FFFFFF'
                    }
                }
            },
            scales: {
                y: {
                    ticks: {
                        color: 'rgb(255, 255, 255)'
                    },
                    grid: {
                        color: 'rgb(92, 92, 92)'
                    }
                },
                x: {
                    ticks: {
                        color: 'rgb(255, 255, 255)'
                    },
                    grid: {
                        color: 'rgb(92, 92, 92)'
                    }
                }
            },
        },
        plugins: [htmlLegendPlugin]
    });

    const htmlLegendPlugin1 = {
        id: 'htmlLegend',
        afterUpdate(chart, args, options) {
          const ul = getOrCreateLegendList(chart, options.containerID);
          ul.style.flexWrap = 'wrap';
      
          // Remove old legend items
          while (ul.firstChild) {
            ul.firstChild.remove();
          }
      
          // Reuse the built-in legendItems generator
          const items = chart.options.plugins.legend.labels.generateLabels(chart);
      
          items.forEach(item => {
            const li = document.createElement('li');
            li.style.alignItems = 'center';
            li.style.cursor = 'pointer';
            li.style.display = 'flex';
            li.style.flexDirection = 'row';
            li.style.marginLeft = '10px';
      
            li.onclick = () => {
              const {type} = chart.config;
              if (type === 'pie' || type === 'doughnut') {
                // Pie and doughnut charts only have a single dataset and visibility is per item
                chart.toggleDataVisibility(item.index);
              } else {
                chart.setDatasetVisibility(item.datasetIndex, !chart.isDatasetVisible(item.datasetIndex));
              }
              chart.update();
            };
      
            // Color box
            const boxSpan = document.createElement('span');
            boxSpan.style.background = item.fillStyle;
            // boxSpan.style.background = item.strokeStyle;
            boxSpan.style.borderColor = item.strokeStyle;
            boxSpan.style.borderWidth = item.lineWidth + 'px';
            boxSpan.style.display = 'inline-block';
            boxSpan.style.height = '20px';
            boxSpan.style.marginRight = '10px';
            boxSpan.style.width = '20px';
      
            // Text
            const textContainer = document.createElement('p');
            textContainer.style.color = item.fontColor;
            textContainer.style.margin = 0;
            textContainer.style.padding = 0;
            textContainer.style.textDecoration = item.hidden ? 'line-through' : '';
      
            const text = document.createTextNode(item.text);
            textContainer.appendChild(text);
      
            li.appendChild(boxSpan);
            li.appendChild(textContainer);
            ul.appendChild(li);
          });
        }
    };

    myChart1 = new Chart(ctx1, {
        type: 'pie',
        data: {
            labels: ['WXA', 'SSG', 'BDT', 'NFX', 'XGV', 'EBC', 'KJN', 'PHY', 'OTY', 'CSB'],
            datasets: [
              {
                label: '最近兩週',
                data: [10,32,31,27,8,36,17,28,29,33]
              }
            ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            datalabels: {
                display: true,
                color: '#FFFFFF', // 标签文本颜色
                anchor: 'end', // 锚点位置
                clamp: true,
                align: 'end', // 文本对齐方式
                offset: 10, // 偏移量
                font: {
                  size: 12, // 字体大小
                  weight: 1 // 字体粗细
                },
                formatter: (value, context) => { // 自定义标签文本
                  return context.chart.data.labels[context.dataIndex] + ': ' + value;
                }
            },
            htmlLegend: {
                containerID: 'legend-container1'
            },
            legend: {
                display: false,
                labels: {
                    color: '#FFFFFF'
                }
            }
          },
          layout: {
            padding: {
                top: 30,
                bottom: 30
            }
          },
        },
        plugins: [htmlLegendPlugin1]
    });
    
    getYearsAndMonths();
    getWorkNumbers();
    // getWorkNumbersTA001();
    // let fake = '[{"analysisId":31,"agvId":1,"year":2022,"month":12,"day":1,"week":4,"workingMinute":600,"openMinute":840,"task":42}'+
    //             ',{"analysisId":32,"agvId":1,"year":2022,"month":12,"day":2,"week":5,"workingMinute":600,"openMinute":840,"task":40},'+
    //             '{"analysisId":33,"agvId":1,"year":2022,"month":12,"day":3,"week":6,"workingMinute":480,"openMinute":840,"task":33},'+
    //             '{"analysisId":34,"agvId":1,"year":2022,"month":12,"day":4,"week":7,"workingMinute":360,"openMinute":840,"task":22},'+
    //             '{"analysisId":35,"agvId":1,"year":2022,"month":12,"day":5,"week":1,"workingMinute":540,"openMinute":840,"task":34},'+
    //             '{"analysisId":36,"agvId":1,"year":2022,"month":12,"day":6,"week":2,"workingMinute":720,"openMinute":840,"task":41},'+
    //             '{"analysisId":37,"agvId":1,"year":2022,"month":12,"day":7,"week":3,"workingMinute":720,"openMinute":840,"task":49},'+
    //             '{"analysisId":38,"agvId":1,"year":2022,"month":12,"day":8,"week":4,"workingMinute":360,"openMinute":840,"task":28},'+
    //             '{"analysisId":39,"agvId":1,"year":2022,"month":12,"day":9,"week":5,"workingMinute":480,"openMinute":840,"task":35},'+
    //             '{"analysisId":40,"agvId":1,"year":2022,"month":12,"day":10,"week":6,"workingMinute":540,"openMinute":840,"task":34},'+
    //             '{"analysisId":41,"agvId":1,"year":2022,"month":12,"day":11,"week":7,"workingMinute":660,"openMinute":840,"task":42},'+
    //             '{"analysisId":42,"agvId":1,"year":2022,"month":12,"day":12,"week":1,"workingMinute":720,"openMinute":840,"task":44},'+
    //             '{"analysisId":43,"agvId":1,"year":2022,"month":12,"day":13,"week":2,"workingMinute":600,"openMinute":840,"task":38},'+
    //             '{"analysisId":44,"agvId":1,"year":2022,"month":12,"day":14,"week":3,"workingMinute":360,"openMinute":840,"task":22},'+
    //             '{"analysisId":45,"agvId":1,"year":2022,"month":12,"day":15,"week":4,"workingMinute":420,"openMinute":840,"task":29},'+
    //             '{"analysisId":46,"agvId":1,"year":2022,"month":12,"day":16,"week":5,"workingMinute":480,"openMinute":840,"task":32},'+
    //             '{"analysisId":47,"agvId":1,"year":2022,"month":12,"day":17,"week":6,"workingMinute":429,"openMinute":840,"task":34},'+
    //             '{"analysisId":48,"agvId":1,"year":2022,"month":12,"day":18,"week":7,"workingMinute":432,"openMinute":840,"task":38},'+
    //             '{"analysisId":49,"agvId":1,"year":2022,"month":12,"day":19,"week":1,"workingMinute":733,"openMinute":840,"task":23},'+
    //             '{"analysisId":50,"agvId":1,"year":2022,"month":12,"day":20,"week":2,"workingMinute":344,"openMinute":840,"task":40},'+
    //             '{"analysisId":51,"agvId":1,"year":2022,"month":12,"day":21,"week":3,"workingMinute":544,"openMinute":840,"task":38},'+
    //             '{"analysisId":52,"agvId":1,"year":2022,"month":12,"day":22,"week":4,"workingMinute":433,"openMinute":840,"task":55},'+
    //             '{"analysisId":53,"agvId":1,"year":2022,"month":12,"day":23,"week":5,"workingMinute":733,"openMinute":840,"task":45},'+
    //             '{"analysisId":54,"agvId":1,"year":2022,"month":12,"day":24,"week":6,"workingMinute":293,"openMinute":840,"task":32},'+
    //             '{"analysisId":55,"agvId":1,"year":2022,"month":12,"day":25,"week":7,"workingMinute":530,"openMinute":840,"task":29},'+
    //             '{"analysisId":56,"agvId":1,"year":2022,"month":12,"day":26,"week":1,"workingMinute":649,"openMinute":840,"task":34},'+
    //             '{"analysisId":57,"agvId":1,"year":2022,"month":12,"day":27,"week":2,"workingMinute":740,"openMinute":840,"task":38},'+
    //             '{"analysisId":58,"agvId":1,"year":2022,"month":12,"day":28,"week":3,"workingMinute":623,"openMinute":840,"task":42},'+
    //             '{"analysisId":59,"agvId":1,"year":2022,"month":12,"day":29,"week":4,"workingMinute":720,"openMinute":840,"task":48},'+
    //             '{"analysisId":60,"agvId":1,"year":2022,"month":12,"day":30,"week":5,"workingMinute":532,"openMinute":840,"task":33},'+
    //             '{"analysisId":61,"agvId":1,"year":2022,"month":12,"day":31,"week":6,"workingMinute":666,"openMinute":840,"task":40}]';
    // let fakedata = JSON.parse(fake);
    // test(fakedata);
}

function getYearsAndMonths(){
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/analysis/yyyymm", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var data = JSON.parse(this.responseText);
            var html = "";
            for(let i=0;i<data.length;i++){
                var yyyy = data[i].year+"";
                var mm = data[i].month+"";
                mm = (mm.length == 1) ? '0'+mm : mm;
                html+="<li><a class=\"dropdown-item\" onclick=\"reSet("+yyyy+mm+")\">"+data[i].year+"/"+data[i].month+"</a></li>";
            }
            html+="<li><a class=\"dropdown-item\" onclick=\"reSet('all')\">全部</a></li>";
            document.getElementById("yearsandmonths").innerHTML = html;
            reSet('recently');
        }
    };
}

function getWorkNumbers(){
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/analysis/workNumber?month=2", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var data = JSON.parse(this.responseText);
            updateWorknumbers(data);
        }
    };
}

function updateWorknumbers(data){
    myChart1.data.labels = [];
    myChart1.data.datasets[0].data = [];
    data.forEach(workNumberData => {
        myChart1.data.labels.push(workNumberData.workNumber);
        myChart1.data.datasets[0].data.push(workNumberData.count);
    });
    myChart1.update();
}

function getWorkNumbersTA001(){
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/analysis/workNumberTA001?month=2", true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var data = JSON.parse(this.responseText);
            updateWorknumbersTA001(data);
        }
    };
}

function updateWorknumbersTA001(data){
    myChart1.data.labels = [];
    myChart1.data.datasets[0].data = [];
    data.forEach(workNumberData => {
        myChart1.data.labels.push(workNumberData.TA001);
        myChart1.data.datasets[0].data.push(workNumberData.count);
    });
    myChart1.update();
}

function setAGVBtn() {
    return new Promise((resolve, reject) => {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', baseUrl + "/api/homepage/agvlist", true);
        xhr.send();
        xhr.onload = function(){
            if(xhr.status == 200){
                agvList = JSON.parse(this.responseText);
                addAGVBtn(agvList);
                resolve(); // 解析成功时，将 Promise 设置为已完成状态
            }else {
                reject(new Error('AGV列表獲取失敗')); // 解析失败时，将 Promise 设置为拒绝状态
            }
        };
    });
}

function addAGVBtn(agvList){  // 更新資料
    var agvListHTML = "";
    for(let i=0;i<agvList.length;i++){
        agvListHTML += '<input type="radio" class="btn-check" name="btnradio" id="btnradio'+(i+1)+'" autocomplete="off" onclick="agvReset(this)">' +
                        '<label class="btn btn-outline-warning" for="btnradio'+(i+1)+'">' + agvList[i].name + '</label>';
    }
    document.getElementById("agvOption").innerHTML = agvListHTML;
    document.getElementById('btnradio1').checked = true;
}

function reSet(x){
    // 先移除所有按鈕的 active 類
    $(".btn-group .btn").removeClass("active");

    // 根據選擇的值添加相應按鈕的 active 類
    if (x === 'recently') {
        $(".btn-group .btn[data-time='recently']").addClass("active");
    } else {
        $(".btn-group .btn[data-time='month']").addClass("active");
    }
    // 取得哪台AGV被選中
    var btnGroup = document.querySelector('#agvOption');
    var radioButtons = btnGroup.querySelectorAll('input[type="radio"]');
    // 遍历单选按钮列表，找出被选中的按钮
    var selectedRadioButton;
    var selectedId;
    radioButtons.forEach(function(radioButton) {
        if (radioButton.checked) {
            selectedRadioButton = radioButton;
            return;
        }
    });
    if (selectedRadioButton)
        selectedId = selectedRadioButton.id;
    // console.log("IDDDD: ", selectedId);
    // xhr.open('GET', baseUrl + "/api/analysis/mode?value=" + x, true);
    option = x;
    var xhr = new XMLHttpRequest();
    xhr.open('GET', baseUrl + "/api/analysis/mode?agvId="+ (selectedId.slice(-1) == 4 ? 1 : selectedId.slice(-1)) +"&value=" + option, true);
    xhr.send();
    xhr.onload = function(){
        if(xhr.status == 200){
            var data = JSON.parse(this.responseText);
            // console.log(data);
            test(data);
        }
    };
}

function agvReset(el){
    document.getElementById("printOutAGV").innerHTML = el.nextElementSibling.innerHTML;
    const mode = el.id.slice(-1);
    setAGV();
    reSet('recently');
}

function test(data) {
//                var len = myChart.data.labels.length;
    var task_sum = 0;
    var work_sum = 0;
    var open_sum = 0;
    var x=0;
    myChart.data.labels = [];
    myChart.data.datasets[0].data = [];
    myChart.data.datasets[1].data = [];
    var html = "";
    for(let i=0 ; i < data.length ; i++) {
        myChart.data.labels.push(data[i].month + '/' + data[i].day);
        myChart.data.datasets[0].data.push(((data[i].workingMinute/data[i].openMinute)*100).toString().substring(0,2));
        myChart.data.datasets[1].data.push(data[i].task);
        task_sum += data[i].task;
        work_sum += data[i].workingMinute;
        open_sum += data[i].openMinute;
        if(data[i].task>0)x++;
        let week = "";
        let datawd = "";
        switch(data[i].week){
            case 1:
                week = "(一)";
                datawd = "f";
                break;
            case 2:
                week = "(二)";
                datawd = "f";
                break;
            case 3:
                week = "(三)";
                datawd = "f";
                break;
            case 4:
                week = "(四)";
                datawd = "f";
                break;
            case 5:
                week = "(五)";
                datawd = "f";
                break;
            case 6:
                week = "(六)";
                datawd = "t";
                break;
            case 7:
                week = "(日)";
                datawd = "t";
                break;
        }
        // 列印之表格
        html += "<tr id=\""+data[i].month.toString()+data[i].day.toString()+"\"><th scope=\"row\">"+data[i].month + '/' + data[i].day+week+"</th><td>"+data[i].task+"</td><td>"+
                ((data[i].workingMinute/data[i].openMinute)*100).toString().substring(0,2)+"%</td><td>"+data[i].workingMinute+"</td><td>"+data[i].openMinute+"</td>"+
                "<td><input class=\"hh\" data-wd=\""+datawd+"\" type=\"checkbox\" id=\""+data[i].month.toString()+data[i].day.toString()+"d\" checked/></td></th>";
    }
    myChart.update();
    document.getElementById("task_sum").value = task_sum;
    document.getElementById("task").value = String((task_sum/x)*100).substring(0,2);
    document.getElementById("open_sum").value = String(Math.round(open_sum/60))+"hr";
    document.getElementById("work_sum").value = String(Math.round(work_sum/60))+"hr";
    document.getElementById("work").value = String(Math.round(work_sum/x))+"hr";
    document.getElementById("rate").value = String((work_sum/open_sum)*100).substring(0,2)+"%";
    document.getElementById("pt").innerHTML = html;
    // console.log(myChart.data);
    // 選取方塊
    $(function(){
        // 全選 or 全取消
        $('#checkAll').click(function(event) {
            var tr_checkbox = $('table tbody tr').find('input[type=checkbox]');
            tr_checkbox.prop('checked', $(this).prop('checked'));
            // 阻止向上冒泡，以防再次觸發點擊操作
            event.stopPropagation();
        });
        $('#cancelweekend').click(function(event) {
            var tr_checkbox = $('table tbody tr').find('input[data-wd="t"]');
            tr_checkbox.prop('checked', !$(this).prop('checked'));
            var tbr = $('table tbody tr');
            var tbrr = $('.hh');
            $('#checkAll').prop('checked', tbr.find('input[type=checkbox]:checked').length == tbrr.length ? true : false);
            // 阻止向上冒泡，以防再次觸發點擊操作
            event.stopPropagation();
        });
        // 點擊表格每一行的checkbox，表格所有選中的checkbox數 = 表格行數時，則將表頭的‘checkAll’單選框置為選中，否則置為未選中
        $('table tbody tr').find('input[type=checkbox]').click(function(event) {
            var tbr = $('table tbody tr');
            var tbrr = $('.hh');
            $('#checkAll').prop('checked', tbr.find('input[type=checkbox]:checked').length == tbrr.length ? true : false);
            // 阻止向上冒泡，以防再次觸發點擊操作
            event.stopPropagation();
        });
        // 點擊表格行(行内任意位置)，觸發選中或取消選中該行的checkbox
        $('table tbody tr').click(function() {
            $(this).find('input[type=checkbox]').click();
        });

    });
}

//  列印
function printOut() {
    $('.hh').each(function(index, elem) {
        if (!$(elem).prop('checked')) {
          console.log($(this));
          $(this).parent().parent().remove();
        }
    });
    $('input[type=checkbox]').remove();
    
    var sumAndAvgs = document.querySelectorAll('.parse');
    sumAndAvgs.forEach(function(sumAndAvg) {
        sumAndAvg.style.color = "#000000";
        console.log("0000: ", sumAndAvg);
    });
    var printOutContent = document.getElementById("printt").innerHTML;
    var printOutContent1 = document.getElementById("summ").innerHTML;
    document.body.innerHTML = printOutContent+printOutContent1;
    var table = document.getElementById("pt");
    var len = document.getElementById("pt").rows.length;
    var llen=0;
    console.log(len);
    for(let i=0;i<len;i++)
        if(table.rows[i].innerHTML != "")llen++;
    console.log(llen);
    var task_sum = 0;
    var rate_sum = 0;
    var work_sum = 0;
    var open_sum = 0;
    for(let i=0;i<len;i++){
        if(table.rows[i].innerHTML != ""){
            task_sum += Number(table.rows[i].cells[1].innerHTML);
            rate_sum += Number(String(table.rows[i].cells[2].innerHTML).replace("%",""));
            work_sum += Number(table.rows[i].cells[3].innerHTML);
            open_sum += Number(table.rows[i].cells[4].innerHTML);
        }
    }
    console.log("task_sum: "+task_sum);
    console.log("rate_sum "+rate_sum);
    console.log("open_sum "+open_sum);
    document.getElementById("task_sum").value = task_sum;
    document.getElementById("task").value = String((task_sum/len)*100).substring(0,2);
    document.getElementById("open_sum").value = String(open_sum)+"hr";
    document.getElementById("work_sum").value = String(work_sum)+"hr";
    document.getElementById("work").value = String(work_sum/len).substring(0,4)+"hr";
    document.getElementById("rate").value = String(Math.round(rate_sum/len)).substring(0,2)+"%";
    window.print();
    window.location.reload();
}

function myexcel(){
    $('input[type=checkbox]').remove();
    $('#logog').remove();
    var html = '<meta http-equiv="content-type" content="application/vnd.ms-excel; charset=UTF-8" /><title>Excel</title>';
    html += '';
    html += document.getElementById('printt').innerHTML + '';
    window.open('data:application/vnd.ms-excel,' + encodeURIComponent(html));
    window.location.reload();
}

function setAGV(){
    const order = document.getElementById("orderAnalysis");
    order.style.display = "none";
    const agv = document.getElementById("agvAnalysis");
    agv.style.display = "block";
    const agvTimeOption = document.getElementById("agvTimeOption");
    agvTimeOption.style.display = "inline-flex";
    const printBTN = document.getElementById("printBTN");
    printBTN.style.display = "inline-block";
    const exportExcelBTN = document.getElementById("exportExcelBTN");
    exportExcelBTN.style.display = "inline-block";
    const TA001BTN = document.getElementById("TA001BTN");
    TA001BTN.style.display = "none";
}

function setOrder(){
    const agv = document.getElementById("agvAnalysis");
    agv.style.display = "none";
    const order = document.getElementById("orderAnalysis");
    order.style.display = "block";
    const agvTimeOption = document.getElementById("agvTimeOption");
    agvTimeOption.style.display = "none";
    const printBTN = document.getElementById("printBTN");
    printBTN.style.display = "none";
    const exportExcelBTN = document.getElementById("exportExcelBTN");
    exportExcelBTN.style.display = "none";
    const TA001BTN = document.getElementById("TA001BTN");
    TA001BTN.style.display = "inline-block";
}