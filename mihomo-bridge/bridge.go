package mihomo

import (
	C "github.com/metacubex/mihomo/constant"
	"github.com/metacubex/mihomo/hub/executor"
	"github.com/metacubex/mihomo/listener/inbound"
	"github.com/metacubex/mihomo/tunnel"

	_ "golang.org/x/mobile/bind"
)

var currentTun *inbound.Tun

// Start 由 Kotlin 调用（Java 类名 Mihomo）。
// homeDir    : App 的 filesDir（GeoIP.dat/GeoSite.dat 放这里）
// configPath : 配置文件路径（yaml）
// fd         : VpnService establish() 后拿到的 fd
// stack      : "gvisor" | "system" | "mixed"
func Start(homeDir string, configPath string, fd int, stack string) string {
	C.SetHomeDir(homeDir)

	cfg, err := executor.ParseWithPath(configPath)
	if err != nil {
		return "ParseWithPath: " + err.Error()
	}
	cfg.General.Tun.Enable = false
	executor.ApplyConfig(cfg, true)

	st, ok := C.StackTypeMapping[stack]
	if !ok {
		st = C.TunGvisor
	}

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

// Version 返回内核版本，用于连通性自检。
func Version() string {
	return C.Version
}
