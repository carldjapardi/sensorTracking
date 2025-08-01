import argparse
import torch
from ronin.source.model_resnet1d import ResNet1D, BasicBlock1D, FCOutputModule


def get_model(arch: str):
    """Create a ResNet1D model matching the given architecture."""
    if arch == "resnet18":
        return ResNet1D(6, 2, BasicBlock1D, [2, 2, 2, 2], output_block=FCOutputModule, fc_dim=512, trans_planes=128, in_dim=7)
    elif arch == "resnet50":
        return ResNet1D(6, 2, BasicBlock1D, [3, 4, 6, 3], output_block=FCOutputModule, fc_dim=1024)
    else:
        raise ValueError(f"Unsupported architecture: {arch}")


def export(checkpoint: str, out_path: str, seq_len: int, arch: str):
    model = get_model(arch)
    ckpt = torch.load(checkpoint, map_location="cpu")
    state = ckpt.get("model_state_dict", ckpt)
    model.load_state_dict(state)
    model.eval()
    dummy = torch.zeros(1, 6, seq_len)
    torch.onnx.export(
        model,
        dummy,
        out_path,
        input_names=["imu"],
        output_names=["delta_xy"],
        dynamic_axes={"imu": {2: "seq_len"}, "delta_xy": {1: "seq_len"}},
        opset_version=12,
    )


def main():
    parser = argparse.ArgumentParser(description="Export a RONIN checkpoint to ONNX")
    parser.add_argument("checkpoint", help="Path to .pt checkpoint")
    parser.add_argument("out", help="Output ONNX model path")
    parser.add_argument("--arch", default="resnet18", help="Model architecture (resnet18 or resnet50)")
    parser.add_argument("--seq-len", type=int, default=200, help="Sequence length used for export")
    args = parser.parse_args()
    export(args.checkpoint, args.out, args.seq_len, args.arch)


if __name__ == "__main__":
    main()