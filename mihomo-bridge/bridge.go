//go:build android

// Package main 是 gomobile bind 的入口。
// gomobile 要求入口 package 必须是 main，并会生成同名的 Java 类。
package main

import (
	C "github.com/metacubex/mihomo/constant"
	"github.com/metacubex/mihomo/hub/executor"
	"github.com/metacubex/mihomo/listener/inbound"
	"github.com/metacubex/mihomo/tunnel"
)

var currentTun *inbound.Tun

// Start 由 Kotlin 调用。
// homeDir    : App 的 filesDir（GeoIP.dat/GeoSite.dat 应放这里）
// configPath : 配置文件路径（yaml）
// fd         : VpnService establish() 后 detachFd()/fd 的返回值
// stack      : "gvisor" | "system" | "mixed"
//
// 返回 "" 表示成功，否则返回错误信息。
func Start(homeDir string, configPath string, fd int, stack string) string {
	// ① 设置配置根目录（必须在 Parse 之前）
	C.SetHomeDir(homeDir)

	// ② 解析配置
	cfg, err := executor.ParseWithPath(configPath)
	if err != nil {
		return "ParseWithPath: " + err.Error()
	}

	// ③ 关闭内核自建 tun（正确路径是 cfg.General.Tun，不是 cfg.Tun）
	cfg.General.Tun.Enable = false
	executor.ApplyConfig(cfg, true)

	// ④ 用 StackTypeMapping 拿枚举
	st, ok := C.StackTypeMapping[stack]
	if !ok {
		st = C.TunGvisor
	}

	// ⑤ 构造 TunOption 并注入 fd
	tunOpt := &inbound.TunOption{
		BaseOption: inbound.BaseOption{
			NameStr: "tun-in",
		},
		Stack:          st,
		DNSHijack:      []string{"any:53"},
		AutoRoute:      false,
		MTU:            1500,
		GSO:            false,
		FileDescriptor: fd,
	}

	t, err := inbound.NewTun(tunOpt)
	if err != nil {
		return "NewTun: " + err.Error()
	}

	// ⑥ 真正启动（漏这步永不工作）
	if err := t.Listen(tunnel.Tunnel); err != nil {
		return "Listen: " + err.Error()
	}

	currentTun = t
	return ""
}

// Stop 关闭隧道。
func Stop() string {
	if currentTun != nil {
		if err := currentTun.Close(); err != nil {
			currentTun = nil
			return err.Error()
		}
		currentTun = nil
	}
	return ""
}

// Version 返回内核版本，用来做连通性自检。
func Version() string {
	return C.Version
}

func main() {}
