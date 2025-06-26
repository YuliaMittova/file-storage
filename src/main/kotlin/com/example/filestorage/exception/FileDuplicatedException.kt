package com.example.filestorage.exception

class FileDuplicatedException(message: String) : FileStorageException("Duplicated file detected: $message")