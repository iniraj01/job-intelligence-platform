$uri = "http://localhost:8080/api/uploadResume"
$filePath = "test_resume.pdf"

# Read file
$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileName = "test_resume.pdf"

# Create boundary
$boundary = [System.Guid]::NewGuid().ToString()

# Create the request
$httpRequest = [System.Net.HttpWebRequest]::Create($uri)
$httpRequest.Method = "POST"
$httpRequest.ContentType = "multipart/form-data; boundary=$boundary"
$httpRequest.Accept = "application/json"
$httpRequest.Accept = "application/json"
$httpRequest.Headers.Add("Accept-Language", "en-US")

# Build the body
$body = New-Object System.IO.MemoryStream

# Add form field
$bodyText = "--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"$fileName`"`r`nContent-Type: application/pdf`r`n`r`n"
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyText)
$body.Write($bodyBytes, 0, $bodyBytes.Length)

# Add file content
$body.Write($fileBytes, 0, $fileBytes.Length)

# Add closing boundary
$footerText = "`r`n--$boundary--`r`n"
$footerBytes = [System.Text.Encoding]::UTF8.GetBytes($footerText)
$body.Write($footerBytes, 0, $footerBytes.Length)

$httpRequest.ContentLength = $body.Length
$requestStream = $httpRequest.GetRequestStream()
$body.WriteTo($requestStream)
$requestStream.Flush()
$requestStream.Close()

# Get response
try {
    $response = $httpRequest.GetResponse()
    $responseStream = $response.GetResponseStream()
    $streamReader = New-Object System.IO.StreamReader($responseStream)
    $result = $streamReader.ReadToEnd()
    $streamReader.Close()
    $response.Close()
    Write-Host "Response: $result"
} catch {
    Write-Host "Error: $_"
}
