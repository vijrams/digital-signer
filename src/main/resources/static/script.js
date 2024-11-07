 
  $('document').ready(function(){
	$("#v2-sign").hide()
	$("#sign-file").on('change' , function() {
		var signFile = document.getElementById('sign-file');
		$("#s-file-chosen").text(signFile.files[0].name)
		$(".s-file-chosen").show()
		$(".s-file-ctrl").hide()
		$("#s-err").text('')
	});
	
	$("#verify-file").on('change' , function() {
		var verifyFile = document.getElementById('verify-file');
		$("#v1-file-chosen").text(verifyFile.files[0].name)
		$(".v1-file-chosen").show()
		$(".v1-file-ctrl").hide()
		$("#v-err").text('')
	});
	
	$("#verify-sign-file").on('change' , function() {
		var vSignFile = document.getElementById('verify-sign-file');
		$("#v2-file-chosen").text(vSignFile.files[0].name)
		$(".v2-file-chosen").show()
		$(".v2-file-ctrl").hide()
	});	
	
	$("#s-del").click(function(){
		$(".s-file-chosen").hide()
		$(".s-file-ctrl").show()
		$('#sign-file').val('')
	});
	
	$("#v1-del").click(function(){
		$(".v1-file-chosen").hide()
		$(".v1-file-ctrl").show()
		$('#verify-file').val('')
	});
	
	$("#v2-del").click(function(){
		$(".v2-file-chosen").hide()
		$(".v2-file-ctrl").show()
		$('#verify-sign-file').val('')
	});
  
	$("#v_combined_file").on('change' , function() {
		var chkBtn = document.getElementById('v_combined_file');
		console.log(chkBtn.checked)
		if(chkBtn.checked){
			$("#v2-sign").hide()
			$("#empty").show()
		} else{
			$("#v2-sign").show()
			$("#empty").hide()
		}
	});
  
	$(".close-btn").click(function(){
		$("#results").hide()
	});
	
	$("#verify-btn").click(function(){
		var chkBtn = document.getElementById('v_combined_file');
		var verifyFile = document.getElementById('verify-file');
		var verifySignFile = document.getElementById('verify-sign-file');
		if(chkBtn.checked && $('#verify-file').val() == '')
			$("#v-err").text("File is required")
		else if(!chkBtn.checked && ($('#verify-file').val() == '' || $('#verify-sign-file').val() == ''))
			$("#v-err").text("File and signature is required")
		else {
			$("#v-err").text('')
			var fData = new FormData();
			fData.append("file",verifyFile.files[0])
			fData.append("signature",verifySignFile.files[0])
			fData.append("combined_file",chkBtn.checked)
			$.ajax({
				url: "/api/verify", 
				type: "POST",
				data: fData,
				processData: false,
				contentType: false,
				error: function(result){
				    $("#v-err").text(result.responseJSON.error)
				    console.log(result)
				},
				success: function(result){
				    if(result.valid){
                        $('.success_status').show()
                        $('.fail_status').hide()
                    } else {
                        $('.success_status').hide()
                        $('.fail_status').show()
				    }
				    $('#signDesc').text(result.desc)
                    $('#signStatus').text(result.status)
                    $('#timeVerified').text(result.timeVerified)
                    $('#hashAlgorithm').text(result.hashAlgorithm)
                    $('#signAlgorithm').text(result.signatureAlgorithm)
                    $('#keyLength').text(result.keyLength)
                    $('#origFile').text(result.origFilename)
                    $('#timeCreated').text(result.timeCreated)
                    $("#results").show()
                    console.log(result)
				}
			});			
		}
	});

	$("#sign-btn").click(function(){
		var chkBtn = document.getElementById('s_combined_file');
		var signFile = document.getElementById('sign-file');
		console.log(chkBtn.checked)
		if($('#sign-file').val() == '')
			$("#s-err").text("File is required")
		else {
			$("#s-err").text('')
			var fData = new FormData();
			fData.append("file",signFile.files[0])
			fData.append("combined_file",chkBtn.checked)
			$.ajax({
				url: "/api/sign", 
				type: "POST",
				data: fData,
				processData: false,
				contentType: false,
				xhrFields: {
					responseType: 'blob'
				},
				error: function(xhr){
				    console.log(xhr.getResponseHeader('errorMsg'))
				    $("#s-err").text(xhr.getResponseHeader('errorMsg'))
				},
				success: function(result, status, xhr){
					var filename = "";
					var disposition = xhr.getResponseHeader('Content-Disposition');
					if (disposition && disposition.indexOf('attachment') !== -1) {
						var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
						var matches = filenameRegex.exec(disposition);
						if (matches != null && matches[1]) filename = matches[1].replace(/['"]/g, '');
					}
					
					var url = URL.createObjectURL(result);
					var $a = $('<a />', {
					  'href': url,
					  'download': filename,
					  'text': "click"
					}).hide().appendTo("body")[0].click();

					URL.revokeObjectURL(url);
					location.reload();

				}
			});
		}
			
	});

	
  });