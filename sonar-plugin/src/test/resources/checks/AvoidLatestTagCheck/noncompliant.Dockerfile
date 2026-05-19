# Non-compliant cases for GCI1031.
FROM node # Noncompliant {{Pin the base image to a specific tag — an implicit `:latest` ties builds to a moving target.}}
FROM ubuntu:latest # Noncompliant {{Pin the base image to a specific tag — using `:latest` ties builds to a moving target.}}
FROM debian:LATEST # Noncompliant {{Pin the base image to a specific tag — using `:latest` ties builds to a moving target.}}
FROM registry.example.com:5000/lib/nginx # Noncompliant {{Pin the base image to a specific tag — an implicit `:latest` ties builds to a moving target.}}
FROM gcr.io/distroless/static:latest # Noncompliant {{Pin the base image to a specific tag — using `:latest` ties builds to a moving target.}}

