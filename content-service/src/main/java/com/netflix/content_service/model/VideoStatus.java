package com.netflix.content_service.model;
/*
*  tracks video processing life/cycle
* pending -> uploaded -> encoding -> encoded ->ready /failed
* */
public enum VideoStatus {

    PENDING,UPLOADED,ENCODING,
    ENCODED,READY,FAILED

}
