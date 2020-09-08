package com.filestore.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


import com.filestore.model.FileInfo;
import com.filestore.message.ResponseMessage;
import com.filestore.service.FilesStorageService;


@RestController
@RequestMapping("/filestore")
@Api(value="File Store")
public class FilesController {

  @Autowired
  FilesStorageService storageService;

  @ApiOperation(value = "Upload tar files")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Uploaded the file successfully:filename"),
          @ApiResponse(code = 401, message = "Unsupported file format"),
          @ApiResponse(code = 403, message = "Could not upload the file:filename"),
          @ApiResponse(code = 404, message = "Missing file for upload")
  }
  )
  @PostMapping("/upload")
  public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file) {
    String message;
    
    if (null == file.getOriginalFilename()) {
    	message = "Missing file for upload";
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
	}
    
    try {
    	
     String tarFileCheck=storageService.tarValidate(file.getOriginalFilename());
  
     if(tarFileCheck=="valid"){
      storageService.save(file);

      message = "Uploaded the file successfully: " + file.getOriginalFilename();
      return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
      }
     else
     {
         message = "Unsupported file format";
         return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
      }
    } catch (Exception e) {
      message = "Could not upload the file: " + file.getOriginalFilename() + "!";
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
    }
  }

  
  @ApiOperation(value = "List all files", response=FileInfo.class)
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "List of files and access urls"),

          @ApiResponse(code = 404, message = "error: Not found")
  }
  )
  @GetMapping("/files")
  public ResponseEntity<List<FileInfo>> getListFiles() {
    List<FileInfo> fileInfos = storageService.loadAll().map(path -> {
      String filename = path.getFileName().toString();
      String url = MvcUriComponentsBuilder
          .fromMethodName(FilesController.class, "getFile", path.getFileName().toString()).build().toString();

      return new FileInfo(filename, url);
    }).collect(Collectors.toList());
  
    return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
  }

  @ApiOperation(value = "List all files", response=FileInfo.class)
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "File Downloaded"),
          @ApiResponse(code = 404, message = "error: Not found")
  }
  )
  @GetMapping("/files/{filename:.+}")
  @ResponseBody
  public ResponseEntity<Resource> getFile(@PathVariable String filename) {
    Resource file = storageService.load(filename);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
  }
}