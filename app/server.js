const express = require('express');
const mysql = require('mysql');
const app = express();
const port = 3000;

// Ustawienia połączenia z MySQL
const db = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'musicmap'
});

// Połącz się z bazą danych
db.connect((err) => {
//    if (err) {
//        throw err;
//    }
    console.log('MySQL Connected...');
});

// Endpoint do pobierania znaczników
app.get('/markers', (req, res) => {
    let sql = 'SELECT * FROM markers';
    db.query(sql, (err, results) => {
//        if (err) {
//            throw err;
//        }
        res.json(results);
    });
});

// Endpoint do dodawania znaczników
app.post('/markers', (req, res) => {
    const newMarker = req.body;
    let sql = 'INSERT INTO markers SET ?';
    db.query(sql, newMarker, (err, result) => {
//        if (err) {
//            throw err;
//        }
        res.json(result);
    });
});

app.listen(port, () => {
    console.log(`Server started on port ${port}`);
});
