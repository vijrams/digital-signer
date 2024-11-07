package com.vijrams.digsig.controller


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Controller
@RequestMapping("/api/")
class DigitalSignerController {
    @Autowired
    private com.vijrams.digsig.service.DigitalSignerService digitalSignerService

    @Operation(summary = "Sign a file", description = "Signs a file and optionally returns a combined signed file.")
    @ApiResponses(value = [
        @ApiResponse(responseCode = "200", description = "File signed successfully",
                content = [@Content(mediaType = "application/octet-stream", schema = @Schema(implementation = Resource.class))]),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    ])

    @RequestMapping(path = "/sign", method = RequestMethod.POST)
    ResponseEntity<Resource> sign(@RequestParam(value = "file", required = true) MultipartFile file,
                                  @RequestParam(value = "combined_file", defaultValue = "true") boolean combined_file) {
        try {
            def fileInfo = ["origFilename": file.originalFilename, "origFilesize": file.size, "origFiletype": file.contentType]
            byte[] signedFile = digitalSignerService.signFile(file.getBytes(), fileInfo, combined_file)
            ByteArrayResource resource = new ByteArrayResource(signedFile)
            def filename = combined_file?file.originalFilename.replaceFirst('(.*)\\.', '$1.signed.') : file.originalFilename + ".signature"
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=$filename")
                    .contentLength(signedFile.length)
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource)
        } catch (Exception e) {
            println e.message
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .header("errorMsg", e.message)
                    .body("{\"error\": \"${e.message}\"}")
        }
    }

    @RequestMapping(path = "/verify", method = RequestMethod.POST)
    ResponseEntity<Resource> verify(@RequestParam(value = "file", required = true) MultipartFile file,
                                    @RequestParam(value = "signature", required = false) MultipartFile signature,
                                    @RequestParam(value = "combined_file", defaultValue = "true") boolean combined_file) {
        try {
            if(!combined_file && signature == null) { throw new Exception("Signature file is required for separate signature verification") }
            String response = digitalSignerService.verifySignature(file.getBytes(), signature?.getBytes())
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response)
        } catch (Exception e) {
            println e.message
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"${e.message}\"}")
        }
    }

}
