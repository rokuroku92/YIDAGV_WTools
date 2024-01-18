
document.addEventListener("DOMContentLoaded", function() {
    const elevatorDiv = document.getElementById('elevatorDiv');
    elevatorDiv.addEventListener('click', forceCancelElevatorCall);
});

function forceCancelElevatorCall(){
    const elevatorDiv = document.getElementById('elevatorDiv');
    const hasClass = elevatorDiv.classList.contains('active');
    console.log(elevatorDiv);
    console.log("Class: ", elevatorDiv.classList);
    if(hasClass){
        confirm("是否強制取消手動電梯模式？");
    }
}