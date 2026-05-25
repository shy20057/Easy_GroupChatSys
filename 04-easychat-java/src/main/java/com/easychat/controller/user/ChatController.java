package com.easychat.controller.user;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.config.Appconfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDTO;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.enums.ResponseCodeEnum;
import com.easychat.entity.po.ChatMessage;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.exception.BusinessException;
import com.easychat.service.ChatMessageService;
import com.easychat.service.ChatSessionUserService;
import com.easychat.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

@RestController("chatController")
@RequestMapping("/chat")
@Slf4j
public class ChatController extends ABaseController {

     //### P33 发送聊天消息01
    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private ChatSessionUserService chatSessionUserService;
    @Resource
    private Appconfig appConfig;

    @RequestMapping("/sendMessage")
    @GlobalInterceptor
    public ResponseVO sendMessage(HttpServletRequest request,
                                  @NotEmpty String contactId,
                                  @NotEmpty @Max(500) String messageContent,
                                  @NotNull Integer messageType,
                                  Long fileSize,
                                  String fileName,
                                  Integer fileType){


        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        ChatMessage chatMessage = ChatMessage.builder()
                .contactId(contactId)
                .messageContent(messageContent)
                .fileName(fileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .messageType(messageType)
                .build();

        MessageSendDTO messageSendDTO = chatMessageService.saveMessage(chatMessage,tokenUserInfoDTO);

        return getSuccessResponseVO(messageSendDTO);
    }

    //## P35 聊天文件上传03
    @RequestMapping("/uploadFile")
    @GlobalInterceptor
    public ResponseVO uploadFile(HttpServletRequest request, @NotNull Long messageId,
                                  @NotNull MultipartFile file,
                                  @NotNull MultipartFile cover){

        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);
        chatMessageService.saveMessageFile(tokenUserInfoDTO.getUserId(),messageId,file,cover);


        return getSuccessResponseVO(null);
    }

    //## P36 聊天文件下载04
    @RequestMapping("/downloadFile")
    @GlobalInterceptor
    public void downloadFile(HttpServletRequest request, HttpServletResponse response,
                                   @NotEmpty String fileId,
                                  @NotNull Boolean showCover){

        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);
        OutputStream out = null;
        FileInputStream in = null;

        try {
            File file = null;
            if(!StringTools.isNumber(fileId)){ //如果不是数字，则下载头像 不是文件id 那就是头像 下载头像
                String avatarFolderName = Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_AVATAR_NAME;
                String avatarPath = appConfig.getProjectFolder() + avatarFolderName + fileId + Constants.IMAGE_SUFFIX;
                if(showCover){
                    avatarPath = avatarPath.replace(Constants.IMAGE_SUFFIX,Constants.COVER_IMAGE_SUFFIX);
                }
                file = new File(avatarPath);
                if(!file.exists()){
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
            }else{
                file = chatMessageService.downloadFile(tokenUserInfoDTO,Long.parseLong(fileId),showCover);
            }

            // 文件下载
            response.setContentType("application/x-msdownload;charset=UTF-8");
            response.setHeader("Content-Disposition","attachment");
            response.setContentLengthLong(file.length());
            in = new FileInputStream( file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();

        }catch (Exception e){
          log.error("下载文件异常",e);
        }finally {
            if(out != null){
                try{
                    out.close();
                }catch(Exception e){
                    log.error("IO异常",e);
                }
            }

            if(in != null){
                try{
                    in.close();
                }catch(Exception e){
                    log.error("IO异常",e);
                }
            }

        }

    }
}
