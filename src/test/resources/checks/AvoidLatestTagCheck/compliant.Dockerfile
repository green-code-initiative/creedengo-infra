# Compliant cases for GCI1031 — base image pinned to a specific tag, digest, or `scratch`.
FROM node:20.11.1-alpine3.19
FROM ubuntu:22.04
FROM debian:bookworm-slim AS build
FROM gcr.io/distroless/static-debian12:nonroot
FROM eclipse-temurin:17.0.10_7-jre-jammy
FROM nginx@sha256:0c860d0ff7344f4ea1c8e7b62b8e2fae5a1faa61c4e02f0d1f0e2c1f1aaaaaaa
FROM scratch

