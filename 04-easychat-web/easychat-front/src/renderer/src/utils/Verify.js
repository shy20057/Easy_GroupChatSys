const regs = {
  // 邮箱校验
  email: /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/,
  // 数字校验（非零开头的数字）
  number: /^\+?[1-9][0-9]*$/,
  // 密码校验（包含数字、字母，且长度8位及以上，可含特殊字符）
  password: /(?=.*\d)(?=.*[a-zA-Z])[\da-zA-Z~!@#$%^&*]{8,}$/,
  // 版本号校验（如x.x、x.x.x等格式）
  version: /^[0-9.]+$/
};

const verify = (rule,value,reg,callback) => {
    if(value){
        if(!reg.test(value)){
            callback(new Error(rule.message));
        }else{
            callback()
        }
    }else{
        callback()
    }
}

const checkPassword = (value) => { 
    return regs.password.test(value)
}

const checkEmail = (value) => { 
    return regs.email.test(value)
}

const password = (rule,value,callback) => { 
    return verify(rule,value,regs.password,callback)
}

const number = (rule,value,callback) => { 
    return verify(rule,value,regs.number,callback)
}

const email = (rule,value,callback) => { 
    return verify(rule,value,regs.email,callback)
}

export default {
    checkPassword,
    checkEmail,
    password,
    number,
    email
}