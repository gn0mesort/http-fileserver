/**
 * Converts server ISO datetimes to client locate datetimes.
 */
function fixTimes() {
    const times = document.querySelectorAll("time");
    for (let time of times)
    {
        time.innerText = new Date(time.dateTime).toLocaleString();
    }
}

document.body.onload = () => fixTimes();