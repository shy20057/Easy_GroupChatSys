const fs = require('fs');
const sqlite3 = require('sqlite3').verbose();
const os = require('os');
const NODE_ENV = process.env.NODE_ENV;
import { add_tables,add_indexex,alter_tables } from './Tables';

const userDir = os.homedir();
console.log(userDir);
const dbFolder = userDir + (NODE_ENV === 'development' ? '/.easychattest/' : '/.easychat/');
console.log(dbFolder);
if(!fs.existsSync(dbFolder)){
    fs.mkdirSync(dbFolder);
}

// 创建一个SQLite数据库连接实例  操作数据库文件local.db
const db = new sqlite3.Database(dbFolder + 'local.db');
const globalColumnsMap = {};

const createTable = () => { 
   return new Promise(async (resolve, reject) => { 
    for(const item of add_tables){
        await new Promise((subResolve, subReject) => {
            db.run(item, function(err) {
                if (err) {
                    console.error('创建表失败:', err.message);
                    subReject(err);
                } else {
                    subResolve();
                }
            });
        });
    }

    for(const item of add_indexex){
        await new Promise((subResolve, subReject) => {
            db.run(item, function(err) {
                if (err) {
                    console.error('创建索引失败:', err.message);
                    subReject(err);
                } else {
                    subResolve();
                }
            });
        });
    }

    for(const item of alter_tables){
        // 先查询表结构
        const fieldList = await queryAll(`PRAGMA table_info(${item.tableName})`,[]);
        const fieldExists = fieldList.some(row => row.name === item.field);
        
        if(!fieldExists){
           
            await new Promise((subResolve, subReject) => {
                db.run(item.sql, function(err) {
                    if (err) {
                        console.error('添加字段失败:', err.message);
                        subReject(err);
                    } else {
                        console.log(`字段 ${item.field} 添加成功`);
                        subResolve();
                    }
                });
            });
        }
    }
    resolve();
})   
}

// 初始化表结构 映射关系
const initTableColumnsMap = async () => {
     let sql = `select * from sqlite_master where type= 'table' and name != 'sqlite_sequence'`;
     let tables = await queryAll(sql, []);
     for (let i = 0; i < tables.length; i++) {
        sql = `PRAGMA table_info(${tables[i].name})`;
        let columns = await queryAll(sql, []);
        const columnsMapItem = {};
        for (let j = 0; j < columns.length; j++) {
            columnsMapItem[toCamelCase(columns[j].name)] = columns[j].name;
        }

     
        globalColumnsMap[tables[i].name] = columnsMapItem;
        
     }
}



const queryAll = (sql, params) => {
    return new Promise((resolve, reject) => {
        const stmt = db.prepare(sql);
        stmt.all(params, function (err, rows) {
             if (err) {
                console.error('查询失败:', err.message);
                resolve([]);
             }
             rows.forEach((item,index)=>{
                rows[index] = convertDbObject2BizObj(item);
             })
             resolve(rows);
        });
        stmt.finalize();
    })
}

const queryOne = (sql, params) => { 
    return new Promise((resolve, reject) => {
        const stmt = db.prepare(sql);
        stmt.get(params, function (err, rows) {
             if (err) {
                console.error('查询失败:', err.message);
                resolve({});
             }
           
             resolve(convertDbObject2BizObj(rows));
             console.log(`执行的sql: ${sql},params:${params},rows:${JSON.stringify(rows)}`)
        });
        stmt.finalize();
    })
}

const queryCount = (sql, params) => { 
   return new Promise((resolve, reject) => {
        const stmt = db.prepare(sql);
        stmt.get(params, function (err, rows) {
             if (err) {
                resolve(0);
             }
             
             resolve(Array.from(Object.values(rows))[0]);
        });
        stmt.finalize();
    })
}

// 将数据库对象转换成业务对象
const convertDbObject2BizObj = (data) => { 
   if(!data){
    return null;
   }

   const bizData = {};
   for(let item in data){
    bizData[toCamelCase(item)] = data[item];
   }
   return bizData;
}

// 下划线转驼峰
const toCamelCase = (str) => { 
  
    return str.replace(/_([a-z])/g, function (match, letter) { 
        return letter.toUpperCase();
    })
}

// 执行sql
const run = (sql, params) => { 
       return new Promise((resolve, reject) => {
        const stmt = db.prepare(sql);// 创建一个预编译的sql
        stmt.run(params, function (err, rows) { // run 将sql填入参数使其完整 并运行
             if (err) {
                //console.error(`执行的sql: ${sql},params:${params},执行失败:${err}`)
                resolve();
             }
            //console.log(`执行的sql: ${sql},params:${params},执行记录数:${this.changes},rows:${JSON.stringify(rows)}`)
             resolve(this.changes); // 返回受影响的行数 -->返回结果
        });
        stmt.finalize(); // 释放资源
    })
}

const insert = (sqlPrefix, tableName, data) => { 
   const columnsMap = globalColumnsMap[tableName];
   const dbColumns = [];
   const params = [];
   for(let item in data){ 
    if(data[item]!=undefined && columnsMap[item]!=undefined){
        dbColumns.push(columnsMap[item]);
        params.push(data[item]);
    }
   }

   const prepare = "?".repeat(dbColumns.length).split("").join(",");
   const sql = `${sqlPrefix} ${tableName} (${dbColumns.join(",")}) VALUES (${prepare})`;
   return run(sql, params);
}


const update = (tableName, data, paramData) => { 
   const columnsMap = globalColumnsMap[tableName]; // globalColumnsMap已经处理成了一个下划线转驼峰的字典 columnMap是一个对象
   const dbColumns = []; // 数据库字段
   const params = []; // 参数
   const whereColumns = []; // where字段
   
   // 遍历 data 对象，确保字段存在且值不为 null/undefined
   for(let item in data){ 
    if(data[item] !== undefined && data[item] !== null && columnsMap[item] !== undefined){
        dbColumns.push(`${columnsMap[item]}=?`);
        params.push(data[item]);
    }
   }

   // 遍历 paramData 对象，确保字段存在且值不为 null/undefined
   for(let item in paramData){ 
    if(paramData[item] !== undefined && paramData[item] !== null && columnsMap[item] !== undefined){
      params.push(paramData[item]);
      whereColumns.push(`${columnsMap[item]}=?`);
    }
   }

   // 修改 SQL 拼接逻辑，避免多余的右括号
   const sql = `update ${tableName} set ${dbColumns.join(",")} ${whereColumns.length > 0 ? 'where ' + whereColumns.join(" and ") : ''}`;
  
   return run(sql, params);
}

const insertOrReplace = (tableName, data) => { 
    return insert("INSERT OR REPLACE INTO",tableName,data)
}

const insertOrIgnore = (tableName, data) => { 
    return insert("INSERT OR IGNORE INTO",tableName,data)
}



const init = () => { 
    db.serialize(async() => { 
        await createTable();
        await initTableColumnsMap();
        console.log('数据库表初始化完成');
    })
}

init();

export {
    run,
    queryAll,
    queryOne,
    queryCount,
    insert,
    insertOrReplace,
    insertOrIgnore,
    update
}