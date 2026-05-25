const formData = ref({})
const formDataRef = ref(null)
const rules = {
    password: [
        { required: true, message: '请输入新密码' },
        { validator: proxy.Verify.password, message: '密码只能是数字，字母，特殊字符8~18位' }
    ],
    rePassword: [
        { required: true, message: '请输入确认密码' },
        { validator: validateRePass, message: '两次输入的密码不一致' }
    ]
}

// 将 validateRePass 函数定义提前
const validateRePass = (rule, value, callback) => {
    if (value !== formData.value.password) {
        callback(new Error('两次输入的密码不一致'));
    } else {
        callback();
    }
}